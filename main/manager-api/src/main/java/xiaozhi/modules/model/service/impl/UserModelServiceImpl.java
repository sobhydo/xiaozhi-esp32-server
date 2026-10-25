package xiaozhi.modules.model.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.SensitiveDataUtils;
import xiaozhi.modules.model.dao.ModelConfigDao;
import xiaozhi.modules.model.dto.ModelConfigBodyDTO;
import xiaozhi.modules.model.dto.ModelConfigDTO;
import xiaozhi.modules.model.dto.ModelProviderDTO;
import xiaozhi.modules.model.dto.ModelTestRequestDTO;
import xiaozhi.modules.model.dto.ModelTestResultDTO;
import xiaozhi.modules.model.dto.ProviderVoiceDTO;
import xiaozhi.modules.model.dto.UserVoiceBodyDTO;
import xiaozhi.modules.model.dto.UserVoiceDTO;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.model.service.ModelProviderService;
import xiaozhi.modules.model.service.UserModelService;
import xiaozhi.modules.timbre.dao.TimbreDao;
import xiaozhi.modules.timbre.entity.TimbreEntity;
import xiaozhi.modules.voiceclone.dao.VoiceCloneDao;
import xiaozhi.modules.voiceclone.entity.VoiceCloneEntity;

@Service
@AllArgsConstructor
public class UserModelServiceImpl implements UserModelService {

    /** Providers a regular user may add, per model type (upper case). */
    static final Map<String, Set<String>> ALLOWED_PROVIDERS = Map.of(
            "LLM", Set.of("openai", "gemini", "claude"),
            "ASR", Set.of("openai", "elevenlabs", "elevenlabs_stream"),
            "TTS", Set.of("openai", "elevenlabs"));

    static final int USER_MODEL_SORT = 1000;
    private static final int MODEL_NAME_MAX = 50;
    static final int MAX_VOICES_PER_MODEL = 50;
    static final String DEFAULT_VOICE_LANGUAGES = "English, Arabic";
    static final String DEFAULT_OUTPUT_DIR = "tmp/";
    static final String DEFAULT_OPENAI_ASR_URL = "https://api.openai.com/v1/audio/transcriptions";
    static final String DEFAULT_OPENAI_TTS_URL = "https://api.openai.com/v1/audio/speech";

    private final ModelConfigDao modelConfigDao;
    private final ModelConfigService modelConfigService;
    private final ModelProviderService modelProviderService;
    private final ModelConnectionTester connectionTester;
    private final TimbreDao timbreDao;
    private final VoiceCloneDao voiceCloneDao;
    private final RedisUtils redisUtils;

    static boolean isAllowed(String modelType, String provideCode) {
        if (StringUtils.isAnyBlank(modelType, provideCode)) {
            return false;
        }
        return ALLOWED_PROVIDERS.getOrDefault(modelType.toUpperCase(), Set.of()).contains(provideCode);
    }

    /** User-supplied endpoints must be public: the voice server calls them at runtime (SSRF guard). */
    static void assertPublicEndpoints(JSONObject config) {
        if (config == null) {
            return;
        }
        for (String key : List.of("base_url", "url", "api_url", "api_base")) {
            String value = config.getStr(key);
            if (StringUtils.isNotBlank(value)) {
                try {
                    ModelConnectionTester.assertPublicUrl(value);
                } catch (IllegalArgumentException e) {
                    throw new RenException(key + ": " + e.getMessage());
                }
            }
        }
    }

    /** ASR/TTS: fields the voice server needs; the frontend sends them, the backend tolerates their absence. */
    static void applySpeechDefaults(String modelType, String provideCode, JSONObject config) {
        boolean asr = "ASR".equalsIgnoreCase(modelType);
        boolean tts = "TTS".equalsIgnoreCase(modelType);
        if (!asr && !tts) {
            return;
        }
        if (StringUtils.isBlank(config.getStr("output_dir"))) {
            config.set("output_dir", DEFAULT_OUTPUT_DIR);
        }
        if ("openai".equals(provideCode)) {
            if (asr && StringUtils.isBlank(config.getStr("base_url"))) {
                config.set("base_url", DEFAULT_OPENAI_ASR_URL);
            }
            if (tts && StringUtils.isBlank(config.getStr("api_url"))) {
                config.set("api_url", DEFAULT_OPENAI_TTS_URL);
            }
        }
    }

    @Override
    public List<ModelProviderDTO> getAllowedProviders(String modelType) {
        if (StringUtils.isBlank(modelType)) {
            return List.of();
        }
        return modelProviderService.getListByModelType(modelType).stream()
                .filter(p -> isAllowed(modelType, p.getProviderCode()))
                .toList();
    }

    @Override
    public List<ModelConfigDTO> listOwn(String modelType, Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<ModelConfigEntity> entities = modelConfigDao.selectList(new QueryWrapper<ModelConfigEntity>()
                .eq("user_id", userId)
                .eq(StringUtils.isNotBlank(modelType), "model_type", modelType)
                .orderByDesc("create_date"));
        return entities.stream().map(UserModelServiceImpl::toMaskedDto).toList();
    }

    @Override
    public ModelConfigDTO addOwn(String modelType, String provideCode, ModelConfigBodyDTO body, Long userId) {
        if (StringUtils.isAnyBlank(modelType, provideCode)) {
            throw new RenException(ErrorCode.MODEL_TYPE_PROVIDE_CODE_NOT_NULL);
        }
        if (userId == null) {
            throw new RenException(ErrorCode.UNAUTHORIZED);
        }
        if (!isAllowed(modelType, provideCode)) {
            throw new RenException(ErrorCode.MODEL_PROVIDER_NOT_EXIST);
        }
        if (body == null || StringUtils.isBlank(body.getModelName())) {
            throw new RenException(ErrorCode.PARAMS_GET_ERROR);
        }
        List<ModelProviderDTO> providers = modelProviderService.getList(modelType, provideCode);
        if (providers == null || providers.isEmpty()) {
            throw new RenException(ErrorCode.MODEL_PROVIDER_NOT_EXIST);
        }

        JSONObject config = body.getConfigJson() == null ? new JSONObject() : new JSONObject(body.getConfigJson());
        config.set("type", provideCode);
        applySpeechDefaults(modelType, provideCode, config);
        assertPublicEndpoints(config);

        ModelConfigEntity entity = new ModelConfigEntity();
        entity.setId(DefaultIdentifierGenerator.getInstance().nextUUID(ModelConfigEntity.class));
        entity.setModelType(providers.get(0).getModelType());
        entity.setModelCode(provideCode);
        entity.setModelName(StringUtils.left(body.getModelName().trim(), MODEL_NAME_MAX));
        entity.setIsDefault(0);
        entity.setIsEnabled(Integer.valueOf(0).equals(body.getIsEnabled()) ? 0 : 1);
        entity.setConfigJson(config);
        entity.setRemark(body.getRemark());
        entity.setSort(USER_MODEL_SORT);
        entity.setUserId(userId);
        modelConfigDao.insert(entity);
        if ("TTS".equalsIgnoreCase(entity.getModelType())) {
            insertDefaultVoice(entity.getId(), config.getStr("voice"));
        }
        return toMaskedDto(entity);
    }

    @Override
    public ModelConfigDTO editOwn(String id, ModelConfigBodyDTO body, Long userId) {
        ModelConfigEntity original = requireOwned(id, userId);
        if (body == null) {
            throw new RenException(ErrorCode.PARAMS_GET_ERROR);
        }
        String type = original.getConfigJson() == null ? null : original.getConfigJson().getStr("type");
        if (StringUtils.isBlank(type)) {
            type = original.getModelCode();
        }
        // Users cannot change provider type, default flag or sort
        if (body.getConfigJson() != null) {
            JSONObject config = new JSONObject(body.getConfigJson());
            config.set("type", type);
            assertPublicEndpoints(config);
            body.setConfigJson(config);
        }
        body.setIsDefault(null);
        body.setSort(original.getSort());
        if (body.getModelName() != null) {
            body.setModelName(StringUtils.isBlank(body.getModelName()) ? null
                    : StringUtils.left(body.getModelName().trim(), MODEL_NAME_MAX));
        }
        if (body.getIsEnabled() != null && body.getIsEnabled() != 0) {
            body.setIsEnabled(1);
        }
        // Partial updates (e.g. only isEnabled) keep the stored values, also in the returned DTO
        if (body.getModelName() == null) {
            body.setModelName(original.getModelName());
        }
        if (body.getIsEnabled() == null) {
            body.setIsEnabled(original.getIsEnabled());
        }
        if (body.getRemark() == null) {
            body.setRemark(original.getRemark());
        }
        // Reuses the admin edit path: keeps stored secrets when masked values come back, clears cache
        return modelConfigService.edit(original.getModelType(), type, id, body);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwn(String id, Long userId) {
        requireOwned(id, userId);
        // Refuses when an agent still references the model; clears cache
        modelConfigService.delete(id);
        // The model's own voices and clone slots go with it
        List<TimbreEntity> voices = timbreDao.selectList(new QueryWrapper<TimbreEntity>()
                .select("id").eq("tts_model_id", id));
        List<VoiceCloneEntity> clones = voiceCloneDao.selectList(new QueryWrapper<VoiceCloneEntity>()
                .select("id").eq("model_id", id));
        if (voices != null && !voices.isEmpty()) {
            timbreDao.delete(new QueryWrapper<TimbreEntity>().eq("tts_model_id", id));
            voices.forEach(v -> clearVoiceCache(v.getId()));
        }
        if (clones != null && !clones.isEmpty()) {
            voiceCloneDao.delete(new QueryWrapper<VoiceCloneEntity>().eq("model_id", id));
            clones.forEach(c -> clearVoiceCache(c.getId()));
        }
    }

    @Override
    public List<UserVoiceDTO> listVoices(String modelId, Long userId) {
        requireOwnedTts(modelId, userId);
        List<TimbreEntity> rows = timbreDao.selectList(new QueryWrapper<TimbreEntity>().eq("tts_model_id", modelId));
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .sorted(Comparator.comparing((TimbreEntity t) -> t.getSort() == null ? 0L : t.getSort())
                        .thenComparing(t -> StringUtils.defaultString(t.getName()), String.CASE_INSENSITIVE_ORDER))
                .map(UserModelServiceImpl::toVoiceDto)
                .toList();
    }

    @Override
    public UserVoiceDTO addVoice(String modelId, UserVoiceBodyDTO body, Long userId) {
        requireOwnedTts(modelId, userId);
        if (body == null || StringUtils.isAnyBlank(body.getName(), body.getTtsVoice())) {
            throw new RenException(ErrorCode.PARAMS_GET_ERROR);
        }
        String name = body.getName().trim();
        String ttsVoice = body.getTtsVoice().trim();
        String languages = StringUtils.defaultIfBlank(StringUtils.trim(body.getLanguages()), DEFAULT_VOICE_LANGUAGES);
        if (name.length() > 64 || ttsVoice.length() > 50 || languages.length() > 50) {
            throw new RenException(ErrorCode.PARAMS_GET_ERROR);
        }
        Long count = timbreDao.selectCount(new QueryWrapper<TimbreEntity>().eq("tts_model_id", modelId));
        if (count != null && count >= MAX_VOICES_PER_MODEL) {
            throw new RenException(ErrorCode.VOICE_LIMIT_REACHED, String.valueOf(MAX_VOICES_PER_MODEL));
        }
        TimbreEntity entity = newVoice(modelId, name, ttsVoice, languages);
        timbreDao.insert(entity);
        return toVoiceDto(entity);
    }

    @Override
    public void deleteVoice(String modelId, String voiceId, Long userId) {
        requireOwnedTts(modelId, userId);
        if (StringUtils.isBlank(voiceId)) {
            throw new RenException(ErrorCode.IDENTIFIER_NOT_NULL);
        }
        TimbreEntity voice = timbreDao.selectById(voiceId);
        if (voice == null || !modelId.equals(voice.getTtsModelId())) {
            throw new RenException(ErrorCode.MODEL_NOT_ACCESSIBLE);
        }
        timbreDao.deleteById(voiceId);
        clearVoiceCache(voiceId);
    }

    @Override
    public List<ProviderVoiceDTO> providerVoices(String modelId, Long userId) {
        ModelConfigEntity model = requireOwnedTts(modelId, userId);
        JSONObject config = model.getConfigJson() == null ? new JSONObject() : model.getConfigJson();
        String type = StringUtils.defaultIfBlank(config.getStr("type"), model.getModelCode());
        return connectionTester.listProviderVoices(type, config);
    }

    private ModelConfigEntity requireOwnedTts(String modelId, Long userId) {
        ModelConfigEntity model = requireOwned(modelId, userId);
        if (!"TTS".equalsIgnoreCase(model.getModelType())) {
            throw new RenException(ErrorCode.MODEL_NOT_ACCESSIBLE);
        }
        return model;
    }

    /** One voice row for the model's configured default voice, so it is selectable in agents right away. */
    private void insertDefaultVoice(String modelId, String voice) {
        if (StringUtils.isBlank(voice)) {
            return;
        }
        String code = StringUtils.left(voice.trim(), 50);
        timbreDao.insert(newVoice(modelId, StringUtils.left(code, 64), code, DEFAULT_VOICE_LANGUAGES));
    }

    private static TimbreEntity newVoice(String modelId, String name, String ttsVoice, String languages) {
        TimbreEntity entity = new TimbreEntity();
        entity.setId(DefaultIdentifierGenerator.getInstance().nextUUID(TimbreEntity.class));
        entity.setTtsModelId(modelId);
        entity.setName(name);
        entity.setTtsVoice(ttsVoice);
        entity.setLanguages(languages);
        entity.setSort(0L);
        return entity;
    }

    private void clearVoiceCache(String voiceId) {
        List<String> keys = new ArrayList<>();
        keys.add(RedisKeys.getTimbreDetailsKey(voiceId));
        keys.add(RedisKeys.getTimbreNameById(voiceId));
        redisUtils.delete(keys);
    }

    private static UserVoiceDTO toVoiceDto(TimbreEntity t) {
        return new UserVoiceDTO(t.getId(), t.getName(), t.getTtsVoice(), t.getLanguages());
    }

    @Override
    public ModelTestResultDTO test(ModelTestRequestDTO request, Long userId) {
        if (request == null) {
            return new ModelTestResultDTO(false, "Missing request body", 0);
        }
        String modelType = request.getModelType();
        String provideCode = request.getProvideCode();
        JSONObject config = request.getConfigJson() == null ? new JSONObject()
                : new JSONObject(request.getConfigJson());

        if (StringUtils.isNotBlank(request.getId())) {
            ModelConfigEntity stored = requireOwned(request.getId(), userId);
            JSONObject storedConfig = stored.getConfigJson() == null ? new JSONObject() : stored.getConfigJson();
            for (String key : storedConfig.keySet()) {
                Object submitted = config.get(key);
                boolean missing = submitted == null
                        || (submitted instanceof String s && StringUtils.isBlank(s));
                boolean maskedSecret = SensitiveDataUtils.isSensitiveField(key)
                        && submitted instanceof String s && SensitiveDataUtils.isMaskedValue(s);
                if (missing || maskedSecret) {
                    config.set(key, storedConfig.get(key));
                }
            }
            if (StringUtils.isBlank(modelType)) {
                modelType = stored.getModelType();
            }
            if (StringUtils.isBlank(provideCode)) {
                provideCode = storedConfig.getStr("type");
            }
        }
        if (!isAllowed(modelType, provideCode)) {
            return new ModelTestResultDTO(false, "Test not available for this provider yet", 0);
        }
        return connectionTester.test(modelType, provideCode, config);
    }

    private ModelConfigEntity requireOwned(String id, Long userId) {
        if (StringUtils.isBlank(id)) {
            throw new RenException(ErrorCode.IDENTIFIER_NOT_NULL);
        }
        // DAO read: unmasked, decrypted config
        ModelConfigEntity entity = modelConfigDao.selectById(id);
        if (entity == null || userId == null || !userId.equals(entity.getUserId())) {
            throw new RenException(ErrorCode.MODEL_NOT_ACCESSIBLE);
        }
        return entity;
    }

    private static ModelConfigDTO toMaskedDto(ModelConfigEntity entity) {
        ModelConfigDTO dto = ConvertUtils.sourceToTarget(entity, ModelConfigDTO.class);
        if (dto.getConfigJson() != null) {
            dto.setConfigJson(SensitiveDataUtils.maskSensitiveFields(dto.getConfigJson()));
        }
        return dto;
    }
}

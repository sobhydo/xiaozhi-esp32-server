package xiaozhi.modules.model.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.SensitiveDataUtils;
import xiaozhi.modules.model.dao.ModelConfigDao;
import xiaozhi.modules.model.dto.ModelConfigBodyDTO;
import xiaozhi.modules.model.dto.ModelConfigDTO;
import xiaozhi.modules.model.dto.ModelProviderDTO;
import xiaozhi.modules.model.dto.ModelTestRequestDTO;
import xiaozhi.modules.model.dto.ModelTestResultDTO;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.model.service.ModelProviderService;
import xiaozhi.modules.model.service.UserModelService;

@Service
@AllArgsConstructor
public class UserModelServiceImpl implements UserModelService {

    /** Providers a regular user may add, per model type (upper case). */
    static final Map<String, Set<String>> ALLOWED_PROVIDERS = Map.of(
            "LLM", Set.of("openai", "gemini", "claude"),
            "ASR", Set.of("openai", "elevenlabs"),
            "TTS", Set.of("openai", "elevenlabs"));

    static final int USER_MODEL_SORT = 1000;
    private static final int MODEL_NAME_MAX = 50;

    private final ModelConfigDao modelConfigDao;
    private final ModelConfigService modelConfigService;
    private final ModelProviderService modelProviderService;
    private final ModelConnectionTester connectionTester;

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
        for (String key : List.of("base_url", "url", "api_url")) {
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
    public void deleteOwn(String id, Long userId) {
        requireOwned(id, userId);
        // Refuses when an agent still references the model; clears cache
        modelConfigService.delete(id);
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

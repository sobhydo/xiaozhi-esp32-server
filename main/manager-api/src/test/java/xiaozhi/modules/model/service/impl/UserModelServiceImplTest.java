package xiaozhi.modules.model.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import cn.hutool.json.JSONObject;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.utils.MessageUtils;
import xiaozhi.modules.agent.dao.AgentDao;
import xiaozhi.modules.model.dao.ModelConfigDao;
import xiaozhi.modules.model.dto.ModelConfigBodyDTO;
import xiaozhi.modules.model.dto.ModelConfigDTO;
import xiaozhi.modules.model.dto.ModelProviderDTO;
import xiaozhi.modules.model.dto.ModelTestRequestDTO;
import xiaozhi.modules.model.dto.ModelTestResultDTO;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.model.service.ModelProviderService;
import xiaozhi.modules.model.dto.ProviderVoiceDTO;
import xiaozhi.modules.model.dto.UserVoiceBodyDTO;
import xiaozhi.modules.model.dto.UserVoiceDTO;
import xiaozhi.modules.timbre.dao.TimbreDao;
import xiaozhi.modules.timbre.entity.TimbreEntity;
import xiaozhi.modules.voiceclone.dao.VoiceCloneDao;
import xiaozhi.modules.voiceclone.entity.VoiceCloneEntity;

class UserModelServiceImplTest {

    private ModelConfigDao dao;
    private ModelConfigService modelConfigService;
    private ModelProviderService providerService;
    private ModelConnectionTester tester;
    private TimbreDao timbreDao;
    private VoiceCloneDao voiceCloneDao;
    private RedisUtils redisUtils;
    private UserModelServiceImpl service;
    private MockedStatic<MessageUtils> messages;

    @BeforeEach
    void setUp() {
        dao = mock(ModelConfigDao.class);
        modelConfigService = mock(ModelConfigService.class);
        providerService = mock(ModelProviderService.class);
        tester = mock(ModelConnectionTester.class);
        timbreDao = mock(TimbreDao.class);
        voiceCloneDao = mock(VoiceCloneDao.class);
        redisUtils = mock(RedisUtils.class);
        service = new UserModelServiceImpl(dao, modelConfigService, providerService, tester, timbreDao,
                voiceCloneDao, redisUtils);
        messages = mockStatic(MessageUtils.class);
    }

    @AfterEach
    void tearDown() {
        messages.close();
    }

    private static ModelProviderDTO provider(String code) {
        ModelProviderDTO p = new ModelProviderDTO();
        p.setModelType("LLM");
        p.setProviderCode(code);
        return p;
    }

    private static ModelConfigEntity owned(Long userId) {
        ModelConfigEntity e = new ModelConfigEntity();
        e.setId("m1");
        e.setModelType("LLM");
        e.setModelCode("openai");
        e.setModelName("Mine");
        e.setIsEnabled(1);
        e.setSort(1000);
        e.setUserId(userId);
        e.setConfigJson(new JSONObject().set("type", "openai").set("api_key", "sk-stored-1234567890")
                .set("model_name", "gpt-4o-mini"));
        return e;
    }

    @Test
    void providersAreWhitelisted() {
        when(providerService.getListByModelType("LLM")).thenReturn(
                List.of(provider("openai"), provider("ollama"), provider("claude"), provider("gemini")));
        List<String> codes = service.getAllowedProviders("LLM").stream().map(ModelProviderDTO::getProviderCode)
                .toList();
        assertEquals(List.of("openai", "claude", "gemini"), codes);
    }

    @Test
    void addCreatesOwnedModelWithForcedTypeAndMaskedKey() {
        when(providerService.getList("LLM", "claude")).thenReturn(List.of(provider("claude")));
        ModelConfigBodyDTO body = new ModelConfigBodyDTO();
        body.setModelName("My Claude");
        body.setIsDefault(1);
        body.setSort(1);
        body.setConfigJson(new JSONObject().set("type", "openai").set("api_key", "sk-ant-1234567890abcdef"));

        ModelConfigDTO dto = service.addOwn("LLM", "claude", body, 7L);

        ArgumentCaptor<ModelConfigEntity> saved = ArgumentCaptor.forClass(ModelConfigEntity.class);
        verify(dao).insert(saved.capture());
        ModelConfigEntity e = saved.getValue();
        assertEquals(7L, e.getUserId());
        assertEquals(0, e.getIsDefault());
        assertEquals(1, e.getIsEnabled());
        assertEquals(1000, e.getSort());
        assertEquals("claude", e.getConfigJson().getStr("type"));
        assertEquals("sk-ant-1234567890abcdef", e.getConfigJson().getStr("api_key"));
        assertTrue(dto.getConfigJson().getStr("api_key").contains("****"));
        assertEquals(7L, dto.getUserId());
    }

    @Test
    void addRejectsProviderOutsideWhitelist() {
        ModelConfigBodyDTO body = new ModelConfigBodyDTO();
        body.setModelName("x");
        RenException ex = assertThrows(RenException.class, () -> service.addOwn("LLM", "ollama", body, 7L));
        assertEquals(ErrorCode.MODEL_PROVIDER_NOT_EXIST, ex.getCode());
        verify(dao, never()).insert(any(ModelConfigEntity.class));
    }

    @Test
    void editAndDeleteRequireOwnership() {
        when(dao.selectById("m1")).thenReturn(owned(8L));
        RenException edit = assertThrows(RenException.class,
                () -> service.editOwn("m1", new ModelConfigBodyDTO(), 7L));
        assertEquals(ErrorCode.MODEL_NOT_ACCESSIBLE, edit.getCode());
        RenException del = assertThrows(RenException.class, () -> service.deleteOwn("m1", 7L));
        assertEquals(ErrorCode.MODEL_NOT_ACCESSIBLE, del.getCode());

        ModelConfigEntity system = owned(null);
        when(dao.selectById("sys")).thenReturn(system);
        assertThrows(RenException.class, () -> service.deleteOwn("sys", 7L));
        verify(modelConfigService, never()).delete(anyString());
        verify(modelConfigService, never()).edit(anyString(), anyString(), anyString(), any());
    }

    @Test
    void editKeepsTypeAndDelegatesToMergeLogic() {
        when(dao.selectById("m1")).thenReturn(owned(7L));
        ModelConfigBodyDTO body = new ModelConfigBodyDTO();
        body.setIsEnabled(0);
        body.setIsDefault(1);
        body.setConfigJson(new JSONObject().set("type", "gemini").set("api_key", "sk-s**********7890"));

        service.editOwn("m1", body, 7L);

        ArgumentCaptor<ModelConfigBodyDTO> sent = ArgumentCaptor.forClass(ModelConfigBodyDTO.class);
        verify(modelConfigService).edit(eq("LLM"), eq("openai"), eq("m1"), sent.capture());
        assertEquals("openai", sent.getValue().getConfigJson().getStr("type"));
        assertNull(sent.getValue().getIsDefault());
        assertEquals(0, sent.getValue().getIsEnabled());
        assertEquals("Mine", sent.getValue().getModelName());
        assertEquals(1000, sent.getValue().getSort());
    }

    @Test
    void deleteOwnDelegates() {
        when(dao.selectById("m1")).thenReturn(owned(7L));
        service.deleteOwn("m1", 7L);
        verify(modelConfigService).delete("m1");
    }

    @Test
    void testUsesStoredKeyWhenMasked() {
        when(dao.selectById("m1")).thenReturn(owned(7L));
        when(tester.test(anyString(), anyString(), any())).thenReturn(new ModelTestResultDTO(true, "Connection OK", 5));
        ModelTestRequestDTO req = new ModelTestRequestDTO();
        req.setId("m1");
        req.setModelType("LLM");
        req.setProvideCode("openai");
        req.setConfigJson(new JSONObject().set("api_key", "sk-s**********7890").set("model_name", "gpt-4.1-mini"));

        assertTrue(service.test(req, 7L).isOk());

        ArgumentCaptor<JSONObject> cfg = ArgumentCaptor.forClass(JSONObject.class);
        verify(tester).test(eq("LLM"), eq("openai"), cfg.capture());
        assertEquals("sk-stored-1234567890", cfg.getValue().getStr("api_key"));
        assertEquals("gpt-4.1-mini", cfg.getValue().getStr("model_name"));
    }

    @Test
    void testOfOtherUsersModelIsRefused() {
        when(dao.selectById("m1")).thenReturn(owned(8L));
        ModelTestRequestDTO req = new ModelTestRequestDTO();
        req.setId("m1");
        req.setModelType("LLM");
        req.setProvideCode("openai");
        assertThrows(RenException.class, () -> service.test(req, 7L));
    }

    @Test
    void testOfUnsupportedProviderReportsNotAvailable() {
        ModelTestRequestDTO req = new ModelTestRequestDTO();
        req.setModelType("LLM");
        req.setProvideCode("ollama");
        ModelTestResultDTO r = service.test(req, 7L);
        assertFalse(r.isOk());
        assertEquals("Test not available for this provider yet", r.getMessage());
    }

    @Test
    void assertModelUsableChecksOwner() {
        ModelConfigServiceImpl impl = new ModelConfigServiceImpl(dao, providerService, mock(RedisUtils.class),
                mock(AgentDao.class), timbreDao, voiceCloneDao);
        ModelConfigEntity mine = new ModelConfigEntity();
        mine.setId("m1");
        mine.setUserId(7L);
        when(dao.selectOne(any())).thenReturn(mine);
        impl.assertModelUsable("m1", 7L);
        assertEquals(ErrorCode.MODEL_NOT_ACCESSIBLE,
                assertThrows(RenException.class, () -> impl.assertModelUsable("m1", 8L)).getCode());
        assertEquals(ErrorCode.MODEL_NOT_ACCESSIBLE,
                assertThrows(RenException.class, () -> impl.assertSystemModel("m1")).getCode());

        ModelConfigEntity system = new ModelConfigEntity();
        system.setId("sys");
        when(dao.selectOne(any())).thenReturn(system);
        impl.assertModelUsable("sys", 8L);
        impl.assertSystemModel("sys");
        impl.assertModelUsable(null, 8L);

        when(dao.selectOne(any())).thenReturn(null);
        // Stale ids are tolerated, as before ownership existed
        impl.assertModelUsable("missing", 8L);
    }

    // ---- phase 2: ASR/TTS models, voices ----

    private static ModelProviderDTO speechProvider(String type, String code) {
        ModelProviderDTO p = new ModelProviderDTO();
        p.setModelType(type);
        p.setProviderCode(code);
        return p;
    }

    private static ModelConfigEntity ownedTts(Long userId, String type) {
        ModelConfigEntity e = owned(userId);
        e.setId("t1");
        e.setModelType("TTS");
        e.setModelCode(type);
        e.setConfigJson(new JSONObject().set("type", type).set("api_key", "xi-stored-1234567890")
                .set("voice", "21m00Tcm4TlvDq8ikWAM"));
        return e;
    }

    @Test
    void addTtsCreatesDefaultVoiceAndFillsDefaults() {
        when(providerService.getList("TTS", "openai")).thenReturn(List.of(speechProvider("TTS", "openai")));
        ModelConfigBodyDTO body = new ModelConfigBodyDTO();
        body.setModelName("My OpenAI TTS");
        body.setConfigJson(new JSONObject().set("api_key", "sk-1234567890abcdef").set("voice", "alloy"));

        service.addOwn("TTS", "openai", body, 7L);

        ArgumentCaptor<ModelConfigEntity> saved = ArgumentCaptor.forClass(ModelConfigEntity.class);
        verify(dao).insert(saved.capture());
        JSONObject cfg = saved.getValue().getConfigJson();
        assertEquals("tmp/", cfg.getStr("output_dir"));
        assertEquals("https://api.openai.com/v1/audio/speech", cfg.getStr("api_url"));

        ArgumentCaptor<TimbreEntity> voice = ArgumentCaptor.forClass(TimbreEntity.class);
        verify(timbreDao).insert(voice.capture());
        assertEquals(saved.getValue().getId(), voice.getValue().getTtsModelId());
        assertEquals("alloy", voice.getValue().getTtsVoice());
        assertEquals("alloy", voice.getValue().getName());
        assertEquals("English, Arabic", voice.getValue().getLanguages());
        assertEquals(32, voice.getValue().getId().length());
        assertEquals(0L, voice.getValue().getSort());
    }

    @Test
    void addTtsWithoutVoiceSkipsDefaultVoice() {
        when(providerService.getList("TTS", "elevenlabs")).thenReturn(List.of(speechProvider("TTS", "elevenlabs")));
        ModelConfigBodyDTO body = new ModelConfigBodyDTO();
        body.setModelName("EL");
        body.setConfigJson(new JSONObject().set("api_key", "xi-1234567890").set("output_dir", "out/"));
        service.addOwn("TTS", "elevenlabs", body, 7L);
        ArgumentCaptor<ModelConfigEntity> saved = ArgumentCaptor.forClass(ModelConfigEntity.class);
        verify(dao).insert(saved.capture());
        assertEquals("out/", saved.getValue().getConfigJson().getStr("output_dir"));
        verify(timbreDao, never()).insert(any(TimbreEntity.class));
    }

    @Test
    void addAsrFillsOpenAiDefaults() {
        when(providerService.getList("ASR", "openai")).thenReturn(List.of(speechProvider("ASR", "openai")));
        ModelConfigBodyDTO body = new ModelConfigBodyDTO();
        body.setModelName("Whisper");
        body.setConfigJson(new JSONObject().set("api_key", "sk-1234567890").set("model_name", "whisper-1"));
        service.addOwn("ASR", "openai", body, 7L);
        ArgumentCaptor<ModelConfigEntity> saved = ArgumentCaptor.forClass(ModelConfigEntity.class);
        verify(dao).insert(saved.capture());
        assertEquals("https://api.openai.com/v1/audio/transcriptions", saved.getValue().getConfigJson().getStr("base_url"));
        assertEquals("tmp/", saved.getValue().getConfigJson().getStr("output_dir"));
        verify(timbreDao, never()).insert(any(TimbreEntity.class));
    }

    @Test
    void apiBaseIsCheckedForSsrf() {
        assertThrows(RenException.class, () -> UserModelServiceImpl
                .assertPublicEndpoints(new JSONObject().set("api_base", "http://127.0.0.1:8080/v1")));
        UserModelServiceImpl.assertPublicEndpoints(new JSONObject().set("api_base", "https://8.8.8.8/v1"));
    }

    @Test
    void listVoicesSortsBySortThenName() {
        when(dao.selectById("t1")).thenReturn(ownedTts(7L, "elevenlabs"));
        TimbreEntity b = voice("v2", "t1", "beta", 0L);
        TimbreEntity a = voice("v1", "t1", "Alpha", 0L);
        TimbreEntity z = voice("v3", "t1", "aaa", 5L);
        when(timbreDao.selectList(any())).thenReturn(List.of(z, b, a));
        List<UserVoiceDTO> list = service.listVoices("t1", 7L);
        assertEquals(List.of("v1", "v2", "v3"), list.stream().map(UserVoiceDTO::getId).toList());
        assertEquals("Alpha", list.get(0).getName());
    }

    private static TimbreEntity voice(String id, String modelId, String name, Long sort) {
        TimbreEntity t = new TimbreEntity();
        t.setId(id);
        t.setTtsModelId(modelId);
        t.setName(name);
        t.setTtsVoice("code-" + id);
        t.setLanguages("English");
        t.setSort(sort);
        return t;
    }

    @Test
    void voiceEndpointsRequireOwnedTtsModel() {
        when(dao.selectById("t1")).thenReturn(ownedTts(8L, "elevenlabs"));
        assertEquals(ErrorCode.MODEL_NOT_ACCESSIBLE,
                assertThrows(RenException.class, () -> service.listVoices("t1", 7L)).getCode());
        when(dao.selectById("m1")).thenReturn(owned(7L)); // LLM
        assertEquals(ErrorCode.MODEL_NOT_ACCESSIBLE,
                assertThrows(RenException.class, () -> service.listVoices("m1", 7L)).getCode());
        assertThrows(RenException.class, () -> service.providerVoices("m1", 7L));
    }

    @Test
    void addVoiceValidatesAndDefaultsLanguages() {
        when(dao.selectById("t1")).thenReturn(ownedTts(7L, "elevenlabs"));
        when(timbreDao.selectCount(any())).thenReturn(3L);
        UserVoiceBodyDTO body = new UserVoiceBodyDTO();
        body.setName("  Rachel ");
        body.setTtsVoice(" 21m00Tcm4TlvDq8ikWAM ");
        UserVoiceDTO dto = service.addVoice("t1", body, 7L);
        assertEquals("Rachel", dto.getName());
        assertEquals("21m00Tcm4TlvDq8ikWAM", dto.getTtsVoice());
        assertEquals("English, Arabic", dto.getLanguages());
        verify(timbreDao).insert(any(TimbreEntity.class));

        UserVoiceBodyDTO missing = new UserVoiceBodyDTO();
        missing.setName("x");
        assertThrows(RenException.class, () -> service.addVoice("t1", missing, 7L));
        UserVoiceBodyDTO tooLong = new UserVoiceBodyDTO();
        tooLong.setName("x");
        tooLong.setTtsVoice("v".repeat(51));
        assertThrows(RenException.class, () -> service.addVoice("t1", tooLong, 7L));
    }

    @Test
    void addVoiceEnforcesLimit() {
        when(dao.selectById("t1")).thenReturn(ownedTts(7L, "elevenlabs"));
        when(timbreDao.selectCount(any())).thenReturn(50L);
        UserVoiceBodyDTO body = new UserVoiceBodyDTO();
        body.setName("n");
        body.setTtsVoice("v");
        assertEquals(ErrorCode.VOICE_LIMIT_REACHED,
                assertThrows(RenException.class, () -> service.addVoice("t1", body, 7L)).getCode());
        verify(timbreDao, never()).insert(any(TimbreEntity.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteVoiceOnlyForRowsOfThatModelAndClearsCache() {
        when(dao.selectById("t1")).thenReturn(ownedTts(7L, "elevenlabs"));
        when(timbreDao.selectById("v1")).thenReturn(voice("v1", "t1", "a", 0L));
        when(timbreDao.selectById("vX")).thenReturn(voice("vX", "other-model", "a", 0L));

        assertThrows(RenException.class, () -> service.deleteVoice("t1", "vX", 7L));
        verify(timbreDao, never()).deleteById("vX");

        service.deleteVoice("t1", "v1", 7L);
        verify(timbreDao).deleteById("v1");
        ArgumentCaptor<Collection<String>> keys = ArgumentCaptor.forClass(Collection.class);
        verify(redisUtils).delete(keys.capture());
        assertTrue(keys.getValue().contains("timbre:details:v1"));
        assertTrue(keys.getValue().contains("timbre:name:v1"));
    }

    @Test
    void providerVoicesDelegatesWithStoredConfig() {
        when(dao.selectById("t1")).thenReturn(ownedTts(7L, "elevenlabs"));
        when(tester.listProviderVoices(eq("elevenlabs"), any()))
                .thenReturn(List.of(new ProviderVoiceDTO("id1", "Rachel", "premade", null, null)));
        List<ProviderVoiceDTO> voices = service.providerVoices("t1", 7L);
        assertEquals("id1", voices.get(0).getVoiceId());
        ArgumentCaptor<JSONObject> cfg = ArgumentCaptor.forClass(JSONObject.class);
        verify(tester).listProviderVoices(eq("elevenlabs"), cfg.capture());
        assertEquals("xi-stored-1234567890", cfg.getValue().getStr("api_key"));
    }

    @Test
    void deleteOwnTtsRemovesVoicesAndClonesAfterReferenceCheck() {
        when(dao.selectById("t1")).thenReturn(ownedTts(7L, "elevenlabs"));
        when(timbreDao.selectList(any())).thenReturn(List.of(voice("v1", "t1", "a", 0L)));
        VoiceCloneEntity clone = new VoiceCloneEntity();
        clone.setId("c1");
        when(voiceCloneDao.selectList(any())).thenReturn(List.of(clone));

        service.deleteOwn("t1", 7L);

        verify(modelConfigService).delete("t1");
        verify(timbreDao).delete(any());
        verify(voiceCloneDao).delete(any());
        verify(redisUtils, org.mockito.Mockito.times(2)).delete(anyCollection());
    }

    @Test
    void deleteOwnStopsWhenModelIsReferenced() {
        when(dao.selectById("t1")).thenReturn(ownedTts(7L, "elevenlabs"));
        org.mockito.Mockito.doThrow(new RenException("in use")).when(modelConfigService).delete("t1");
        assertThrows(RenException.class, () -> service.deleteOwn("t1", 7L));
        verify(timbreDao, never()).delete(any());
        verify(voiceCloneDao, never()).delete(any());
    }

    @Test
    void assertVoiceUsableChecksVoiceModelOwnerAndCloneOwner() {
        ModelConfigServiceImpl impl = new ModelConfigServiceImpl(dao, providerService, mock(RedisUtils.class),
                mock(AgentDao.class), timbreDao, voiceCloneDao);
        // ai_tts_voice row of a model owned by user 8
        when(timbreDao.selectOne(any())).thenReturn(voice("v1", "t1", "a", 0L));
        ModelConfigEntity model = new ModelConfigEntity();
        model.setId("t1");
        model.setUserId(8L);
        when(dao.selectOne(any())).thenReturn(model);
        impl.assertVoiceUsable("v1", 8L);
        assertEquals(ErrorCode.MODEL_NOT_ACCESSIBLE,
                assertThrows(RenException.class, () -> impl.assertVoiceUsable("v1", 7L)).getCode());
        // system voice
        model.setUserId(null);
        impl.assertVoiceUsable("v1", 7L);

        // clone rows
        when(timbreDao.selectOne(any())).thenReturn(null);
        VoiceCloneEntity clone = new VoiceCloneEntity();
        clone.setId("c1");
        clone.setUserId(8L);
        when(voiceCloneDao.selectOne(any())).thenReturn(clone);
        impl.assertVoiceUsable("c1", 8L);
        assertEquals(ErrorCode.MODEL_NOT_ACCESSIBLE,
                assertThrows(RenException.class, () -> impl.assertVoiceUsable("c1", 7L)).getCode());

        // unknown ids and blanks are tolerated
        when(voiceCloneDao.selectOne(any())).thenReturn(null);
        impl.assertVoiceUsable("missing", 7L);
        impl.assertVoiceUsable(null, 7L);
        impl.assertVoiceUsable("", 7L);
    }
}

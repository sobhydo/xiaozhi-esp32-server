package xiaozhi.modules.model.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

class UserModelServiceImplTest {

    private ModelConfigDao dao;
    private ModelConfigService modelConfigService;
    private ModelProviderService providerService;
    private ModelConnectionTester tester;
    private UserModelServiceImpl service;
    private MockedStatic<MessageUtils> messages;

    @BeforeEach
    void setUp() {
        dao = mock(ModelConfigDao.class);
        modelConfigService = mock(ModelConfigService.class);
        providerService = mock(ModelProviderService.class);
        tester = mock(ModelConnectionTester.class);
        service = new UserModelServiceImpl(dao, modelConfigService, providerService, tester);
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
                mock(AgentDao.class));
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
}

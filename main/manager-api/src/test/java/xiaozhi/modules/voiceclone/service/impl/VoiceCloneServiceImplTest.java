package xiaozhi.modules.voiceclone.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.json.JSONObject;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.utils.MessageUtils;
import xiaozhi.modules.agent.dao.AgentDao;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.model.service.impl.ModelConnectionTester;
import xiaozhi.modules.sys.dao.SysUserDao;
import xiaozhi.modules.sys.service.SysUserService;
import xiaozhi.modules.voiceclone.dao.VoiceCloneDao;
import xiaozhi.modules.voiceclone.dto.VoiceCloneCreateDTO;
import xiaozhi.modules.voiceclone.dto.VoiceCloneResponseDTO;
import xiaozhi.modules.voiceclone.entity.VoiceCloneEntity;

class VoiceCloneServiceImplTest {

    private VoiceCloneDao dao;
    private ModelConfigService modelConfigService;
    private AgentDao agentDao;
    private RedisUtils redisUtils;
    private ModelConnectionTester tester;
    private SysUserDao sysUserDao;
    private VoiceCloneServiceImpl service;
    private MockedStatic<MessageUtils> messages;

    @BeforeEach
    void setUp() {
        dao = mock(VoiceCloneDao.class);
        modelConfigService = mock(ModelConfigService.class);
        agentDao = mock(AgentDao.class);
        redisUtils = mock(RedisUtils.class);
        tester = mock(ModelConnectionTester.class);
        sysUserDao = mock(SysUserDao.class);
        service = new VoiceCloneServiceImpl(modelConfigService, mock(SysUserService.class), sysUserDao,
                new ObjectMapper(), agentDao, redisUtils, tester);
        ReflectionTestUtils.setField(service, "baseDao", dao);
        messages = mockStatic(MessageUtils.class);
    }

    @AfterEach
    void tearDown() {
        messages.close();
    }

    private static ModelConfigEntity model(String id, Long userId, String type) {
        ModelConfigEntity m = new ModelConfigEntity();
        m.setId(id);
        m.setModelType("TTS");
        m.setModelName("My ElevenLabs");
        m.setUserId(userId);
        m.setConfigJson(new JSONObject().set("type", type).set("api_key", "xi-key")
                .set("api_base", "https://api.elevenlabs.io/v1"));
        return m;
    }

    private static VoiceCloneCreateDTO create(String modelId, String name) {
        VoiceCloneCreateDTO dto = new VoiceCloneCreateDTO();
        dto.setModelId(modelId);
        dto.setName(name);
        return dto;
    }

    @Test
    void createOwnMakesPendingSlotOnOwnElevenLabsModel() {
        when(modelConfigService.getModelByIdFromCache("t1")).thenReturn(model("t1", 7L, "elevenlabs"));
        when(dao.selectCount(any())).thenReturn(0L);

        VoiceCloneResponseDTO dto = service.createOwn(create("t1", "  My voice  "), 7L);

        ArgumentCaptor<VoiceCloneEntity> saved = ArgumentCaptor.forClass(VoiceCloneEntity.class);
        verify(dao).insert(saved.capture());
        VoiceCloneEntity e = saved.getValue();
        assertEquals("t1", e.getModelId());
        assertEquals("My voice", e.getName());
        assertEquals("pending", e.getVoiceId());
        assertEquals("English, Arabic", e.getLanguages());
        assertEquals(7L, e.getUserId());
        assertEquals(0, e.getTrainStatus());
        assertTrue(dto.getSelfManaged());
        assertEquals("My ElevenLabs", dto.getModelName());
        assertFalse(dto.getHasVoice());
    }

    @Test
    void createOwnRejectsForeignSystemOrNonElevenLabsModels() {
        when(modelConfigService.getModelByIdFromCache("other")).thenReturn(model("other", 8L, "elevenlabs"));
        when(modelConfigService.getModelByIdFromCache("sys")).thenReturn(model("sys", null, "elevenlabs"));
        when(modelConfigService.getModelByIdFromCache("oa")).thenReturn(model("oa", 7L, "openai"));
        for (String id : new String[] { "other", "sys", "oa", "missing" }) {
            RenException ex = assertThrows(RenException.class, () -> service.createOwn(create(id, "n"), 7L), id);
            assertEquals(ErrorCode.MODEL_NOT_ACCESSIBLE, ex.getCode(), id);
        }
        verify(dao, never()).insert(any(VoiceCloneEntity.class));
    }

    @Test
    void createOwnEnforcesSlotLimit() {
        when(modelConfigService.getModelByIdFromCache("t1")).thenReturn(model("t1", 7L, "elevenlabs"));
        when(dao.selectCount(any())).thenReturn(20L);
        assertEquals(ErrorCode.VOICE_LIMIT_REACHED,
                assertThrows(RenException.class, () -> service.createOwn(create("t1", "n"), 7L)).getCode());
        verify(dao, never()).insert(any(VoiceCloneEntity.class));
    }

    private static VoiceCloneEntity slot(String id, Long userId, String modelId, int status, String voiceId) {
        VoiceCloneEntity e = new VoiceCloneEntity();
        e.setId(id);
        e.setUserId(userId);
        e.setModelId(modelId);
        e.setTrainStatus(status);
        e.setVoiceId(voiceId);
        return e;
    }

    @Test
    void deleteOwnRemovesTrainedVoiceBestEffort() {
        when(dao.selectById("c1")).thenReturn(slot("c1", 7L, "t1", 2, "elVoice"));
        when(modelConfigService.getModelByIdFromCache("t1")).thenReturn(model("t1", 7L, "elevenlabs"));
        when(agentDao.selectList(any())).thenReturn(List.of());
        when(tester.deleteElevenLabsVoice(anyString(), anyString(), anyString())).thenReturn(false);

        service.deleteOwn("c1", 7L);

        verify(tester).deleteElevenLabsVoice("https://api.elevenlabs.io/v1", "xi-key", "elVoice");
        verify(dao).deleteById("c1");
        verify(redisUtils).delete("timbre:name:c1");
    }

    @Test
    void deleteOwnSkipsProviderForUntrainedSlots() {
        when(dao.selectById("c1")).thenReturn(slot("c1", 7L, "t1", 0, "pending"));
        when(modelConfigService.getModelByIdFromCache("t1")).thenReturn(model("t1", 7L, "elevenlabs"));
        when(agentDao.selectList(any())).thenReturn(List.of());
        service.deleteOwn("c1", 7L);
        verify(tester, never()).deleteElevenLabsVoice(any(), any(), any());
        verify(dao).deleteById("c1");
    }

    @Test
    void deleteOwnRefusesAdminSlotsOtherUsersAndVoicesInUse() {
        // admin-assigned slot on a system model
        when(dao.selectById("admin")).thenReturn(slot("admin", 7L, "sys", 2, "v"));
        when(modelConfigService.getModelByIdFromCache("sys")).thenReturn(model("sys", null, "elevenlabs"));
        assertEquals(ErrorCode.MODEL_NOT_ACCESSIBLE,
                assertThrows(RenException.class, () -> service.deleteOwn("admin", 7L)).getCode());

        // someone else's slot
        when(dao.selectById("foreign")).thenReturn(slot("foreign", 8L, "t1", 0, "pending"));
        assertEquals(ErrorCode.VOICE_RESOURCE_NO_PERMISSION,
                assertThrows(RenException.class, () -> service.deleteOwn("foreign", 7L)).getCode());

        // used by an agent
        when(dao.selectById("used")).thenReturn(slot("used", 7L, "t1", 2, "v"));
        when(modelConfigService.getModelByIdFromCache("t1")).thenReturn(model("t1", 7L, "elevenlabs"));
        AgentEntity agent = new AgentEntity();
        agent.setAgentName("Kitchen");
        when(agentDao.selectList(any())).thenReturn(List.of(agent));
        assertEquals(ErrorCode.VOICE_CLONE_IN_USE,
                assertThrows(RenException.class, () -> service.deleteOwn("used", 7L)).getCode());

        verify(dao, never()).deleteById(anyString());
        verify(tester, never()).deleteElevenLabsVoice(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageMarksSelfManagedSlots() {
        VoiceCloneEntity own = slot("c1", 7L, "t1", 0, "pending");
        VoiceCloneEntity admin = slot("c2", 7L, "sys", 2, "v");
        IPage<VoiceCloneEntity> page = new Page<>(1, 10);
        page.setRecords(List.of(own, admin));
        page.setTotal(2);
        when(dao.selectPage(any(IPage.class), any())).thenReturn(page);
        when(sysUserDao.selectList(any())).thenReturn(List.of());
        when(modelConfigService.getModelByIdFromCache("t1")).thenReturn(model("t1", 7L, "elevenlabs"));
        when(modelConfigService.getModelByIdFromCache("sys")).thenReturn(model("sys", null, "elevenlabs"));

        PageData<VoiceCloneResponseDTO> result = service.pageWithNames(
                new java.util.HashMap<>(Map.of("page", "1", "limit", "10", "userId", "7")));

        assertTrue(result.getList().get(0).getSelfManaged());
        assertFalse(result.getList().get(1).getSelfManaged());
    }

    @Test
    void selfManagedAndMp3Detection() {
        assertTrue(VoiceCloneServiceImpl.isSelfManaged(7L, 7L));
        assertFalse(VoiceCloneServiceImpl.isSelfManaged(null, 7L));
        assertFalse(VoiceCloneServiceImpl.isSelfManaged(8L, 7L));

        assertTrue(VoiceCloneServiceImpl.isMp3("ID3\u0004".getBytes()));
        assertTrue(VoiceCloneServiceImpl.isMp3(new byte[] { (byte) 0xFF, (byte) 0xFB, 0x10 }));
        assertFalse(VoiceCloneServiceImpl.isMp3("RIFF....WAVE".getBytes()));
        assertFalse(VoiceCloneServiceImpl.isMp3(new byte[] { 1 }));
        assertFalse(VoiceCloneServiceImpl.isMp3(null));
    }
}

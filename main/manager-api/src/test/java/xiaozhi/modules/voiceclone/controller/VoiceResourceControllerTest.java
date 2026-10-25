package xiaozhi.modules.voiceclone.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.user.UserDetail;
import xiaozhi.common.utils.MessageUtils;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.security.user.SecurityUser;
import xiaozhi.modules.voiceclone.dto.VoiceCloneResponseDTO;
import xiaozhi.modules.voiceclone.service.VoiceCloneService;

class VoiceResourceControllerTest {

    private VoiceCloneService service;
    private VoiceResourceController controller;
    private MockedStatic<SecurityUser> security;
    private MockedStatic<MessageUtils> messages;

    @BeforeEach
    void setUp() {
        service = mock(VoiceCloneService.class);
        controller = new VoiceResourceController(service, mock(ModelConfigService.class));
        security = mockStatic(SecurityUser.class);
        messages = mockStatic(MessageUtils.class);
        when(service.getByUserIdWithNames(7L)).thenReturn(List.of(new VoiceCloneResponseDTO()));
    }

    @AfterEach
    void tearDown() {
        security.close();
        messages.close();
    }

    private void login(long id, int superAdmin) {
        UserDetail user = new UserDetail();
        user.setId(id);
        user.setSuperAdmin(superAdmin);
        security.when(SecurityUser::getUser).thenReturn(user);
    }

    @Test
    void userMayOnlyListOwnResources() {
        login(7L, 0);
        assertEquals(1, controller.getByUserId(7L).getData().size());

        Result<List<VoiceCloneResponseDTO>> denied = controller.getByUserId(8L);
        assertEquals(ErrorCode.VOICE_RESOURCE_NO_PERMISSION, denied.getCode());
        verify(service, never()).getByUserIdWithNames(8L);
    }

    @Test
    void superAdminMayListAnyUser() {
        login(1L, 1);
        assertEquals(1, controller.getByUserId(7L).getData().size());
    }
}

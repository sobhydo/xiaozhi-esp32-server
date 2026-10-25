package xiaozhi.modules.model.service;

import java.util.List;

import xiaozhi.modules.model.dto.ModelConfigBodyDTO;
import xiaozhi.modules.model.dto.ModelConfigDTO;
import xiaozhi.modules.model.dto.ModelProviderDTO;
import xiaozhi.modules.model.dto.ModelTestRequestDTO;
import xiaozhi.modules.model.dto.ModelTestResultDTO;
import xiaozhi.modules.model.dto.ProviderVoiceDTO;
import xiaozhi.modules.model.dto.UserVoiceBodyDTO;
import xiaozhi.modules.model.dto.UserVoiceDTO;

/**
 * Bring-your-own-key: model entries owned by a single user.
 */
public interface UserModelService {

    List<ModelProviderDTO> getAllowedProviders(String modelType);

    List<ModelConfigDTO> listOwn(String modelType, Long userId);

    ModelConfigDTO addOwn(String modelType, String provideCode, ModelConfigBodyDTO body, Long userId);

    ModelConfigDTO editOwn(String id, ModelConfigBodyDTO body, Long userId);

    void deleteOwn(String id, Long userId);

    ModelTestResultDTO test(ModelTestRequestDTO request, Long userId);

    /** Voices (ai_tts_voice rows) of an own TTS model, by sort then name. */
    List<UserVoiceDTO> listVoices(String modelId, Long userId);

    UserVoiceDTO addVoice(String modelId, UserVoiceBodyDTO body, Long userId);

    void deleteVoice(String modelId, String voiceId, Long userId);

    /** Voices offered by the provider for the own model's key (picker). */
    List<ProviderVoiceDTO> providerVoices(String modelId, Long userId);
}

package xiaozhi.modules.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BYOK: a voice (ai_tts_voice row) of a TTS model owned by the current user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "BYOK: voice of an own TTS model")
public class UserVoiceDTO {

    @Schema(description = "Voice row id (ai_tts_voice.id), used as the agent's ttsVoiceId")
    private String id;

    @Schema(description = "Display name")
    private String name;

    @Schema(description = "Provider voice id sent to the TTS provider")
    private String ttsVoice;

    @Schema(description = "Languages, e.g. \"English, Arabic\"")
    private String languages;
}

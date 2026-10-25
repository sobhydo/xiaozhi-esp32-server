package xiaozhi.modules.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * BYOK: request body to add a voice to an own TTS model.
 */
@Data
@Schema(description = "BYOK: add a voice to an own TTS model")
public class UserVoiceBodyDTO {

    @Schema(description = "Display name (max 64)")
    private String name;

    @Schema(description = "Provider voice id (max 50)")
    private String ttsVoice;

    @Schema(description = "Languages (max 50), default \"English, Arabic\"")
    private String languages;
}

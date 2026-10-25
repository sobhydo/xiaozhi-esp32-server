package xiaozhi.modules.voiceclone.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * BYOK: a user creates a clone slot on their own ElevenLabs TTS model.
 */
@Data
@Schema(description = "BYOK: create a self-managed voice clone slot")
public class VoiceCloneCreateDTO {

    @Schema(description = "Own ElevenLabs TTS model id")
    private String modelId;

    @Schema(description = "Voice name (max 64)")
    private String name;

    @Schema(description = "Languages, default \"English, Arabic\"")
    private String languages;
}

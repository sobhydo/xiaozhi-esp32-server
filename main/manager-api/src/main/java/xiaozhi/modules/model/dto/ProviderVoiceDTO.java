package xiaozhi.modules.model.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BYOK: a voice offered by the provider for the user's key (picker entry).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "BYOK: voice available on the provider")
public class ProviderVoiceDTO {

    @Schema(description = "Provider voice id")
    private String voiceId;

    @Schema(description = "Voice name")
    private String name;

    @Schema(description = "Provider category, e.g. premade / cloned / generated")
    private String category;

    @Schema(description = "Preview audio URL (may be null)")
    private String previewUrl;

    @Schema(description = "Provider labels (accent, gender, ...), may be null")
    private Map<String, Object> labels;
}

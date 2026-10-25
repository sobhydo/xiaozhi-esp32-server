package xiaozhi.modules.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "BYOK: model connection test result")
public class ModelTestResultDTO {

    @Schema(description = "Whether the provider accepted the request")
    private boolean ok;

    @Schema(description = "OK text or the provider's error (max 300 chars)")
    private String message;

    @Schema(description = "Round-trip time in milliseconds")
    private long latencyMs;
}

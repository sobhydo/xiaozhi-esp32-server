package xiaozhi.modules.model.dto;

import cn.hutool.json.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "BYOK: test a model connection")
public class ModelTestRequestDTO {

    @Schema(description = "Existing own model id (optional); its stored key is used when api_key is masked or empty")
    private String id;

    @Schema(description = "Model type, e.g. LLM")
    private String modelType;

    @Schema(description = "Provider code: openai | gemini | claude")
    private String provideCode;

    @Schema(description = "Model config (api_key, model_name, base_url, ...)")
    private JSONObject configJson;
}

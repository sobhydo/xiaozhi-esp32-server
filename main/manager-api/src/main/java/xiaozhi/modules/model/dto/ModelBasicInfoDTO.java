package xiaozhi.modules.model.dto;

import lombok.Data;

@Data
public class ModelBasicInfoDTO {
    private String id;
    private String modelName;
    /** true when the model belongs to the current user (bring-your-own-key) */
    private Boolean owned;
}
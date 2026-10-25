package xiaozhi.modules.model.service;

import java.util.List;

import xiaozhi.modules.model.dto.ModelConfigBodyDTO;
import xiaozhi.modules.model.dto.ModelConfigDTO;
import xiaozhi.modules.model.dto.ModelProviderDTO;
import xiaozhi.modules.model.dto.ModelTestRequestDTO;
import xiaozhi.modules.model.dto.ModelTestResultDTO;

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
}

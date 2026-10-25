package xiaozhi.modules.model.controller;

import java.util.List;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.model.dto.ModelConfigBodyDTO;
import xiaozhi.modules.model.dto.ModelConfigDTO;
import xiaozhi.modules.model.dto.ModelProviderDTO;
import xiaozhi.modules.model.dto.ModelTestRequestDTO;
import xiaozhi.modules.model.dto.ModelTestResultDTO;
import xiaozhi.modules.model.dto.ProviderVoiceDTO;
import xiaozhi.modules.model.dto.UserVoiceBodyDTO;
import xiaozhi.modules.model.dto.UserVoiceDTO;
import xiaozhi.modules.model.service.UserModelService;
import xiaozhi.modules.security.user.SecurityUser;

/**
 * Bring-your-own-key: every user manages their own model entries (own API keys).
 */
@AllArgsConstructor
@RestController
@RequestMapping("/models/mine")
@Tag(name = "My models (BYOK)")
public class UserModelController {

    private final UserModelService userModelService;

    @GetMapping("/providers")
    @Operation(summary = "Providers a user may add for a model type")
    @RequiresPermissions("sys:role:normal")
    public Result<List<ModelProviderDTO>> getProviders(@RequestParam(defaultValue = "LLM") String modelType) {
        return new Result<List<ModelProviderDTO>>().ok(userModelService.getAllowedProviders(modelType));
    }

    @GetMapping
    @Operation(summary = "Models owned by the current user (secrets masked)")
    @RequiresPermissions("sys:role:normal")
    public Result<List<ModelConfigDTO>> list(@RequestParam(required = false) String modelType) {
        return new Result<List<ModelConfigDTO>>().ok(userModelService.listOwn(modelType, SecurityUser.getUserId()));
    }

    @PostMapping("/test")
    @Operation(summary = "Test a model connection")
    @RequiresPermissions("sys:role:normal")
    public Result<ModelTestResultDTO> test(@RequestBody ModelTestRequestDTO request) {
        return new Result<ModelTestResultDTO>().ok(userModelService.test(request, SecurityUser.getUserId()));
    }

    @PostMapping("/{modelType}/{provideCode}")
    @Operation(summary = "Add an own model")
    @RequiresPermissions("sys:role:normal")
    public Result<ModelConfigDTO> add(@PathVariable String modelType, @PathVariable String provideCode,
            @RequestBody ModelConfigBodyDTO body) {
        return new Result<ModelConfigDTO>()
                .ok(userModelService.addOwn(modelType, provideCode, body, SecurityUser.getUserId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit an own model")
    @RequiresPermissions("sys:role:normal")
    public Result<ModelConfigDTO> edit(@PathVariable String id, @RequestBody ModelConfigBodyDTO body) {
        return new Result<ModelConfigDTO>().ok(userModelService.editOwn(id, body, SecurityUser.getUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an own model")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> delete(@PathVariable String id) {
        userModelService.deleteOwn(id, SecurityUser.getUserId());
        return new Result<>();
    }

    @GetMapping("/{modelId}/voices")
    @Operation(summary = "Voices of an own TTS model")
    @RequiresPermissions("sys:role:normal")
    public Result<List<UserVoiceDTO>> listVoices(@PathVariable String modelId) {
        return new Result<List<UserVoiceDTO>>().ok(userModelService.listVoices(modelId, SecurityUser.getUserId()));
    }

    @PostMapping("/{modelId}/voices")
    @Operation(summary = "Add a voice to an own TTS model")
    @RequiresPermissions("sys:role:normal")
    public Result<UserVoiceDTO> addVoice(@PathVariable String modelId, @RequestBody UserVoiceBodyDTO body) {
        return new Result<UserVoiceDTO>().ok(userModelService.addVoice(modelId, body, SecurityUser.getUserId()));
    }

    @DeleteMapping("/{modelId}/voices/{voiceId}")
    @Operation(summary = "Delete a voice of an own TTS model")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> deleteVoice(@PathVariable String modelId, @PathVariable String voiceId) {
        userModelService.deleteVoice(modelId, voiceId, SecurityUser.getUserId());
        return new Result<>();
    }

    @GetMapping("/{modelId}/provider-voices")
    @Operation(summary = "Voices available on the provider for an own TTS model's key")
    @RequiresPermissions("sys:role:normal")
    public Result<List<ProviderVoiceDTO>> providerVoices(@PathVariable String modelId) {
        return new Result<List<ProviderVoiceDTO>>()
                .ok(userModelService.providerVoices(modelId, SecurityUser.getUserId()));
    }
}

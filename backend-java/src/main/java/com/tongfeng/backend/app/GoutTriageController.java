package com.tongfeng.backend.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "痛风发作分诊")
public class GoutTriageController {

	private final GoutTriageService goutTriageService;

	public GoutTriageController(GoutTriageService goutTriageService) {
		this.goutTriageService = goutTriageService;
	}

	@PostMapping("/api/v1/triage/gout-flare")
	@Operation(
			summary = "提交痛风发作分诊问卷",
			description = "根据结构化症状输出居家管理、尽快联系医生或紧急线下就医建议；结果不构成诊断。"
	)
	public ApiResponse<AppContracts.GoutFlareTriageResponse> evaluateGoutFlare(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.GoutFlareTriageRequest request
	) {
		return ApiResponse.success(goutTriageService.evaluate(userId, request));
	}
}

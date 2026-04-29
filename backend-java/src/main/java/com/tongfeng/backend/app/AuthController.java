package com.tongfeng.backend.app;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

	private final HealthAssistantService healthAssistantService;

	public AuthController(HealthAssistantService healthAssistantService) {
		this.healthAssistantService = healthAssistantService;
	}

	@PostMapping("/api/v1/auth/mock-login")
	public ApiResponse<AppContracts.AuthTokenResponse> mockLogin(
			@Valid @RequestBody AppContracts.MockLoginRequest request
	) {
		return ApiResponse.success(healthAssistantService.mockLogin(request));
	}
}

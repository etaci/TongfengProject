package com.tongfeng.backend.app;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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

	@PostMapping("/api/v1/auth/register")
	public ApiResponse<AppContracts.AuthTokenResponse> register(
			@Valid @RequestBody AppContracts.RegisterRequest request
	) {
		return ApiResponse.success(healthAssistantService.register(request));
	}

	@PostMapping("/api/v1/auth/login")
	public ApiResponse<AppContracts.AuthTokenResponse> login(
			@Valid @RequestBody AppContracts.LoginRequest request
	) {
		return ApiResponse.success(healthAssistantService.login(request));
	}

	@PostMapping("/api/v1/auth/logout")
	public ApiResponse<AppContracts.AuthLogoutResponse> logout(
			@RequestAttribute(AuthInterceptor.CURRENT_TOKEN) String token
	) {
		return ApiResponse.success(healthAssistantService.logout(token));
	}

	@GetMapping("/api/v1/auth/session")
	public ApiResponse<AppContracts.AuthSessionInfoResponse> getCurrentSession(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestAttribute(AuthInterceptor.CURRENT_TOKEN) String token
	) {
		return ApiResponse.success(healthAssistantService.getCurrentSessionInfo(userId, token));
	}

	@GetMapping("/api/v1/auth/sessions")
	public ApiResponse<List<AppContracts.AuthActiveSessionResponse>> getActiveSessions(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestAttribute(AuthInterceptor.CURRENT_TOKEN) String token
	) {
		return ApiResponse.success(healthAssistantService.getActiveSessions(userId, token));
	}

	@PutMapping("/api/v1/auth/password")
	public ApiResponse<AppContracts.PasswordChangeResponse> changePassword(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestAttribute(AuthInterceptor.CURRENT_TOKEN) String token,
			@Valid @RequestBody AppContracts.ChangePasswordRequest request
	) {
		return ApiResponse.success(healthAssistantService.changePassword(userId, token, request));
	}

	@DeleteMapping("/api/v1/auth/sessions/{sessionCode}")
	public ApiResponse<AppContracts.AuthSessionRevokeResponse> revokeSession(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestAttribute(AuthInterceptor.CURRENT_TOKEN) String token,
			@PathVariable String sessionCode
	) {
		return ApiResponse.success(healthAssistantService.revokeSession(userId, token, sessionCode));
	}

	@GetMapping("/api/v1/privacy/consents/current")
	public ApiResponse<AppContracts.PrivacyConsentResponse> getCurrentPrivacyConsent(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getCurrentPrivacyConsent(userId));
	}

	@GetMapping("/api/v1/privacy/consents/history")
	public ApiResponse<List<AppContracts.PrivacyConsentResponse>> getPrivacyConsentHistory(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId
	) {
		return ApiResponse.success(healthAssistantService.getPrivacyConsentHistory(userId));
	}

	@PutMapping("/api/v1/privacy/consents/current")
	public ApiResponse<AppContracts.PrivacyConsentResponse> updatePrivacyConsent(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@Valid @RequestBody AppContracts.PrivacyConsentSubmitRequest request
	) {
		return ApiResponse.success(healthAssistantService.updatePrivacyConsent(userId, request));
	}
}

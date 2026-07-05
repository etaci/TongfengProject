package com.tongfeng.backend.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@Tag(name = "认证与隐私")
public class AuthController {

	private final HealthAssistantService healthAssistantService;
	private final AppProperties appProperties;

	public AuthController(HealthAssistantService healthAssistantService, AppProperties appProperties) {
		this.healthAssistantService = healthAssistantService;
		this.appProperties = appProperties;
	}

	@PostMapping("/api/v1/auth/mock-login")
	@Operation(summary = "开发环境模拟登录", description = "仅用于开发联调与测试环境，不应作为真实患者入口。")
	@SecurityRequirements
	public ApiResponse<AppContracts.AuthTokenResponse> mockLogin(
			@Valid @RequestBody AppContracts.MockLoginRequest request,
			HttpServletRequest servletRequest
	) {
		if (!appProperties.isMockLoginEnabled()) {
			throw new BusinessException(AppErrorCode.FEATURE_DISABLED, "开发用 mock-login 当前环境未开放");
		}
		return ApiResponse.success(healthAssistantService.mockLogin(request, buildRequestContext(servletRequest)));
	}

	@PostMapping("/api/v1/auth/register")
	@Operation(summary = "注册正式账号", description = "注册邮箱或手机号账号，并同步写入当前隐私授权版本。")
	@SecurityRequirements
	public ApiResponse<AppContracts.AuthTokenResponse> register(
			@Valid @RequestBody AppContracts.RegisterRequest request,
			HttpServletRequest servletRequest
	) {
		return ApiResponse.success(healthAssistantService.register(request, buildRequestContext(servletRequest)));
	}

	@PostMapping("/api/v1/auth/login")
	@Operation(summary = "账号密码登录", description = "支持邮箱或手机号登录，会返回当前会话、风险等级与安全提示。")
	@SecurityRequirements
	public ApiResponse<AppContracts.AuthTokenResponse> login(
			@Valid @RequestBody AppContracts.LoginRequest request,
			HttpServletRequest servletRequest
	) {
		return ApiResponse.success(healthAssistantService.login(request, buildRequestContext(servletRequest)));
	}

	@PostMapping("/api/v1/auth/verification-codes/request")
	@Operation(summary = "申请验证码", description = "当前支持密码重置与账号验证场景，接口已接入冷却与窗口限流。")
	@SecurityRequirements
	public ApiResponse<AppContracts.VerificationChallengeResponse> requestVerificationCode(
			@Valid @RequestBody AppContracts.VerificationCodeRequest request
	) {
		return ApiResponse.success(healthAssistantService.requestVerificationCode(request));
	}

	@PostMapping("/api/v1/auth/password-reset/confirm")
	@Operation(summary = "确认密码重置", description = "使用验证码完成密码重置，成功后旧密码失效。")
	@SecurityRequirements
	public ApiResponse<AppContracts.AuthLogoutResponse> confirmPasswordReset(
			@Valid @RequestBody AppContracts.PasswordResetConfirmRequest request
	) {
		return ApiResponse.success(healthAssistantService.confirmPasswordReset(request));
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
	@Operation(summary = "查询活动会话", description = "返回当前账号所有有效会话，供设备管理与异常排查使用。")
	public ApiResponse<List<AppContracts.AuthActiveSessionResponse>> getActiveSessions(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestAttribute(AuthInterceptor.CURRENT_TOKEN) String token
	) {
		return ApiResponse.success(healthAssistantService.getActiveSessions(userId, token));
	}

	@PutMapping("/api/v1/auth/password")
	@Operation(summary = "修改密码", description = "修改密码后可选择注销其他设备会话。")
	public ApiResponse<AppContracts.PasswordChangeResponse> changePassword(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestAttribute(AuthInterceptor.CURRENT_TOKEN) String token,
			@Valid @RequestBody AppContracts.ChangePasswordRequest request
	) {
		return ApiResponse.success(healthAssistantService.changePassword(userId, token, request));
	}

	@GetMapping("/api/v1/auth/account-verification/status")
	public ApiResponse<AppContracts.AccountVerificationStatusResponse> getAccountVerificationStatus(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestAttribute(AuthInterceptor.CURRENT_TOKEN) String token
	) {
		return ApiResponse.success(healthAssistantService.getAccountVerificationStatus(userId, token));
	}

	@PostMapping("/api/v1/auth/account-verification/request")
	public ApiResponse<AppContracts.VerificationChallengeResponse> requestAccountVerificationCode(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestAttribute(AuthInterceptor.CURRENT_TOKEN) String token
	) {
		return ApiResponse.success(healthAssistantService.requestAccountVerificationCode(userId, token));
	}

	@PostMapping("/api/v1/auth/account-verification/confirm")
	public ApiResponse<AppContracts.AccountVerificationStatusResponse> confirmAccountVerification(
			@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) String userId,
			@RequestAttribute(AuthInterceptor.CURRENT_TOKEN) String token,
			@Valid @RequestBody AppContracts.VerificationCodeConfirmRequest request
	) {
		return ApiResponse.success(healthAssistantService.confirmAccountVerification(userId, token, request));
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

	private AuthRequestContext buildRequestContext(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		String clientIp = forwardedFor != null && !forwardedFor.isBlank()
				? forwardedFor.split(",")[0].trim()
				: request.getRemoteAddr();
		return new AuthRequestContext(
				clientIp,
				request.getHeader("User-Agent"),
				request.getHeader("X-Device-Fingerprint"),
				request.getHeader("X-Device-Label")
		);
	}
}

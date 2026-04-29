package com.tongfeng.backend.app;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

	public static final String CURRENT_USER_ID = "currentUserId";
	private final HealthAssistantService healthAssistantService;

	public AuthInterceptor(HealthAssistantService healthAssistantService) {
		this.healthAssistantService = healthAssistantService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String authHeader = request.getHeader("Authorization");
		if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
			throw new BusinessException("UNAUTHORIZED", "请在 Authorization 中携带 Bearer Token");
		}
		String token = authHeader.substring("Bearer ".length()).trim();
		UserSession session = healthAssistantService.requireSession(token);
		request.setAttribute(CURRENT_USER_ID, session.userId());
		return true;
	}
}

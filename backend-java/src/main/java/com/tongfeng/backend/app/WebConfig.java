package com.tongfeng.backend.app;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final AuthInterceptor authInterceptor;

	public WebConfig(AuthInterceptor authInterceptor) {
		this.authInterceptor = authInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor)
				.addPathPatterns("/api/**")
				.excludePathPatterns(
						"/api/v1/auth/mock-login",
						"/api/v1/auth/register",
						"/api/v1/auth/login",
						"/api/v1/auth/verification-codes/request",
						"/api/v1/auth/password-reset/confirm",
						"/api/public/error-codes",
						"/api/openapi/**",
						"/v3/api-docs/**",
						"/swagger-ui/**",
						"/swagger-ui.html",
						"/webjars/**",
						"/actuator/**"
				);
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/swagger-ui").setViewName("redirect:/swagger-ui.html");
	}
}

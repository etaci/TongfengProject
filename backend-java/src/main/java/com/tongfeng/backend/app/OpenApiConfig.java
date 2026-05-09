package com.tongfeng.backend.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";
	private static final String JSON_MEDIA_TYPE = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
	private static final String MULTIPART_MEDIA_TYPE = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

	private final ObjectMapper objectMapper;

	public OpenApiConfig(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Bean
	public OpenAPI tongfengOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Tongfeng Backend API")
						.description("痛风主动管理后端 API，覆盖登录鉴权、化验单复盘、饮食分析、家属协同与统一错误响应。")
						.version("2026-05")
						.contact(new Contact().name("Tongfeng Team"))
						.license(new License().name("Proprietary")))
				.servers(List.of(
						new Server().url("http://localhost:8080").description("本地 Java 后端"),
						new Server().url("http://127.0.0.1:8080").description("本地回环地址")
				))
				.components(new Components()
						.addSecuritySchemes(
								SECURITY_SCHEME_NAME,
								new SecurityScheme()
										.name(SECURITY_SCHEME_NAME)
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("session-token")
										.description("当前项目使用 Bearer Session Token，而非 JWT。")
						)
						.addExamples("standardSuccessExample", jsonExample("统一成功响应", """
								{
								  "success": true,
								  "code": "OK",
								  "message": "success",
								  "data": {},
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-demo-success",
								  "path": "/api/demo"
								}
								"""))
						.addExamples("unauthorizedExample", jsonExample("未授权响应", """
								{
								  "success": false,
								  "code": "UNAUTHORIZED",
								  "message": "未授权访问",
								  "data": null,
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-demo-401",
								  "path": "/api/v1/profile"
								}
								"""))
						.addExamples("forbiddenExample", jsonExample("无权限响应", """
								{
								  "success": false,
								  "code": "FORBIDDEN",
								  "message": "无权访问",
								  "data": null,
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-demo-403",
								  "path": "/api/v1/family/patients/demo/weekly-report"
								}
								"""))
						.addExamples("validationExample", jsonExample("参数校验失败响应", """
								{
								  "success": false,
								  "code": "VALIDATION_ERROR",
								  "message": "请求参数校验失败",
								  "data": null,
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-demo-400",
								  "path": "/api/v1/auth/register"
								}
								"""))
						.addExamples("tooManyRequestsExample", jsonExample("限流响应", """
								{
								  "success": false,
								  "code": "VERIFICATION_CODE_COOLDOWN",
								  "message": "请求过于频繁，请在 60 秒后再试",
								  "data": null,
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-demo-429",
								  "path": "/api/v1/auth/verification-codes/request"
								}
								"""))
						.addExamples("internalErrorExample", jsonExample("内部错误响应", """
								{
								  "success": false,
								  "code": "INTERNAL_ERROR",
								  "message": "服务器内部错误，请稍后重试。如需排查，请提供 traceId。",
								  "data": null,
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-demo-500",
								  "path": "/api/v1/lab-reports/analyze"
								}
								"""))
						.addExamples("registerRequestExample", jsonExample("注册请求示例", """
								{
								  "nickname": "real-user",
								  "accountType": "EMAIL",
								  "account": "real-user@example.com",
								  "password": "Password123",
								  "confirmPassword": "Password123",
								  "consent": {
								    "consentVersion": "v1.0",
								    "privacyPolicyVersion": "privacy-v1.0",
								    "privacyAccepted": true,
								    "termsAccepted": true,
								    "medicalDataAuthorized": true,
								    "familyCollaborationAuthorized": true,
								    "notificationAuthorized": true
								  }
								}
								"""))
						.addExamples("loginRequestExample", jsonExample("登录请求示例", """
								{
								  "accountType": "EMAIL",
								  "account": "real-user@example.com",
								  "password": "Password123"
								}
								"""))
						.addExamples("verificationCodeRequestExample", jsonExample("验证码请求示例", """
								{
								  "purpose": "PASSWORD_RESET",
								  "accountType": "EMAIL",
								  "account": "real-user@example.com"
								}
								"""))
						.addExamples("knowledgeAskRequestExample", jsonExample("健康问答请求示例", """
								{
								  "question": "尿酸偏高时今天饮食要注意什么？",
								  "scene": "today-action"
								}
								"""))
						.addExamples("labManualConfirmationRequestExample", jsonExample("人工确认补录示例", """
								{
								  "indicators": [
								    {
								      "code": "UA",
								      "name": "尿酸",
								      "value": 428,
								      "unit": "umol/L",
								      "referenceRange": "208-428",
								      "riskLevel": "YELLOW"
								    },
								    {
								      "code": "CREA",
								      "name": "肌酐",
								      "value": 105,
								      "unit": "umol/L",
								      "referenceRange": "57-111",
								      "riskLevel": "GREEN"
								    }
								  ],
								  "summaryNote": "根据原始化验单手动补录关键指标"
								}
								"""))
						.addExamples("mealAnalyzeMultipartExample", jsonExample("餐盘分析 multipart 字段示例", """
								{
								  "file": "<binary image>",
								  "mealType": "DINNER",
								  "takenAt": "2026-05-09T19:30:00Z",
								  "note": "啤酒、海鲜"
								}
								"""))
						.addExamples("labAnalyzeMultipartExample", jsonExample("化验单上传 multipart 字段示例", """
								{
								  "file": "<binary pdf-or-image>",
								  "reportDate": "2026-05-09"
								}
								"""))
						.addExamples("authTokenResponseExample", jsonExample("登录成功响应示例", """
								{
								  "success": true,
								  "code": "OK",
								  "message": "success",
								  "data": {
								    "sessionCode": "session-demo",
								    "userId": "user-demo",
								    "nickname": "real-user",
								    "authMode": "PASSWORD",
								    "accountType": "EMAIL",
								    "accountIdentifier": "real-user@example.com",
								    "deviceLabel": "Web / Chrome / macOS",
								    "clientIpMasked": "127.0.*.*",
								    "loginRiskLevel": "GREEN",
								    "securityNotices": [],
								    "accountVerified": false,
								    "privacyConsentCompleted": true,
								    "createdAt": "2026-05-09T15:00:00Z",
								    "lastSeenAt": "2026-05-09T15:00:00Z",
								    "token": "session-token-demo",
								    "tokenType": "Bearer",
								    "expiresAt": "2026-06-08T15:00:00Z"
								  },
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-auth-success",
								  "path": "/api/v1/auth/login"
								}
								"""))
						.addExamples("verificationChallengeResponseExample", jsonExample("验证码申请成功示例", """
								{
								  "success": true,
								  "code": "OK",
								  "message": "success",
								  "data": {
								    "challengeCode": "verify-demo",
								    "purpose": "PASSWORD_RESET",
								    "accountType": "EMAIL",
								    "maskedTarget": "r***@example.com",
								    "deliveryChannel": "EMAIL",
								    "deliveryProvider": "SIMULATED",
								    "deliveryStatus": "SIMULATED",
								    "expiresAt": "2026-05-09T15:15:00Z",
								    "simulatedCode": "123456",
								    "message": "当前未配置邮件投递通道，已切换为联调模式。当前响应中会返回模拟验证码，便于联调。"
								  },
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-verify-success",
								  "path": "/api/v1/auth/verification-codes/request"
								}
								"""))
						.addExamples("mealAnalyzeFallbackResponseExample", jsonExample("餐盘分析降级示例", """
								{
								  "success": true,
								  "code": "OK",
								  "message": "success",
								  "data": {
								    "recordId": "meal-demo",
								    "imageUrl": "/api/v1/files/file-demo",
								    "mealType": "DINNER",
								    "takenAt": "2026-05-09T19:30:00Z",
								    "riskLevel": "YELLOW",
								    "purineEstimateMg": null,
								    "items": [],
								    "suggestions": [
								      "AI 图像识别当前不可用，本次不会输出正式食材识别结果。",
								      "请手动补充是否包含酒精、海鲜、动物内脏、浓肉汤等高风险食材。"
								    ],
								    "summary": "当前已切换到安全兜底模式：本次仅保留上传记录，不输出正式食材识别结论或嘌呤估算。",
								    "analysisMode": "SAFE_FALLBACK",
								    "trustNotes": [
								      "本次未调用到 AI 图像识别服务，系统没有对食材做正式识别。"
								    ]
								  },
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-meal-fallback",
								  "path": "/api/v1/meals/analyze"
								}
								"""))
						.addExamples("labAnalyzeFallbackResponseExample", jsonExample("化验单降级示例", """
								{
								  "success": true,
								  "code": "OK",
								  "message": "success",
								  "data": {
								    "reportId": "lab-demo",
								    "reportDate": "2026-05-09",
								    "indicators": [],
								    "overallRiskLevel": "YELLOW",
								    "manualConfirmationRequired": true,
								    "reviewReady": false,
								    "extractionStatus": "MANUAL_CONFIRMATION_REQUIRED",
								    "suggestions": [
								      "AI 子服务当前不可用，本次不会估算或补造化验指标。",
								      "请重新上传更清晰的报告，或直接手动补录多个关键指标后再继续复盘。"
								    ],
								    "trustNotes": [
								      "本次未调用到 AI OCR 服务，系统没有生成任何估算化验值。"
								    ],
								    "summary": "当前已切换到安全兜底模式：系统保留原始报告文件，但不会输出正式 OCR 结论。",
								    "analysisMode": "SAFE_FALLBACK"
								  },
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-lab-fallback",
								  "path": "/api/v1/lab-reports/analyze"
								}
								"""))
						.addExamples("labManualPendingReviewResponseExample", jsonExample("待人工确认复盘示例", """
								{
								  "success": true,
								  "code": "OK",
								  "message": "success",
								  "data": {
								    "reportId": "lab-demo",
								    "reportDate": "2026-05-09",
								    "overallRiskLevel": "YELLOW",
								    "manualConfirmationRequired": true,
								    "reviewReady": false,
								    "reviewStatus": "MANUAL_CONFIRMATION_REQUIRED",
								    "reviewSummary": "当前报告未能稳定识别出可复盘的关键指标，请先人工确认。",
								    "workflowTitle": "LAB_MANUAL_CONFIRMATION_PENDING",
								    "comparisons": [],
								    "manualConfirmationTasks": [
								      {
								        "actionKey": "lab-confirm-clarity",
								        "title": "先确认报告是否清晰完整",
								        "description": "优先确认关键指标名、数值、单位是否可见",
								        "status": "DO_NOW",
								        "priority": "HIGH"
								      }
								    ],
								    "blockedOutputs": [
								      "个人目标值是否达标",
								      "与上次报告的趋势变化"
								    ],
								    "nextActions": [
								      "重新上传更清晰的化验单图片或 PDF，优先保证指标名、数值和单位完整可见。"
								    ],
								    "trustNotes": [
								      "当前报告处于人工确认模式，系统不会用估算值替代真实化验结果。"
								    ],
								    "generatedAt": "2026-05-09T15:00:00Z"
								  },
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-lab-review-pending",
								  "path": "/api/v1/lab-reports/lab-demo/review"
								}
								"""))
						.addExamples("errorCodeCatalogResponseExample", jsonExample("错误码字典响应示例", """
								{
								  "success": true,
								  "code": "OK",
								  "message": "success",
								  "data": {
								    "version": "2026-05",
								    "generatedAt": "2026-05-09T15:00:00Z",
								    "items": [
								      {
								        "code": "UNAUTHORIZED",
								        "httpStatus": 401,
								        "httpStatusText": "UNAUTHORIZED",
								        "category": "AUTH",
								        "retryable": false,
								        "defaultMessage": "未授权访问"
								      }
								    ]
								  },
								  "timestamp": "2026-05-09T15:00:00Z",
								  "traceId": "trace-error-catalog",
								  "path": "/api/public/error-codes"
								}
								"""))
						.addResponses("UnauthorizedError", buildErrorResponse("未登录或登录已失效", "#/components/examples/unauthorizedExample"))
						.addResponses("ForbiddenError", buildErrorResponse("当前账号无权执行该操作", "#/components/examples/forbiddenExample"))
						.addResponses("ValidationError", buildErrorResponse("请求参数校验失败", "#/components/examples/validationExample"))
						.addResponses("TooManyRequestsError", buildErrorResponse("请求过于频繁或账号已被暂时锁定", "#/components/examples/tooManyRequestsExample"))
						.addResponses("InternalServerError", buildErrorResponse("服务内部错误，请结合 traceId 排查", "#/components/examples/internalErrorExample")))
				.tags(List.of(
						new Tag().name("系统元信息").description("公共元信息、OpenAPI 与错误码字典"),
						new Tag().name("认证与隐私").description("注册、登录、会话、账号验证与隐私授权"),
						new Tag().name("健康档案").description("用户档案、文件、记录中心、总览与提醒"),
						new Tag().name("饮食与化验").description("餐盘分析、化验单解析、人工确认与问答"),
						new Tag().name("家属协同").description("家属邀请、权限、代办与周报共享")
				));
	}

	@Bean
	public OperationCustomizer tongfengOperationCustomizer() {
		return (operation, handlerMethod) -> {
			String path = extractPath(handlerMethod);
			applyTag(operation, path);
			applySecurity(operation, path);
			applyDefaultResponses(operation, path);
			applyEndpointExamples(operation, path);
			return operation;
		};
	}

	@Bean
	public OpenApiCustomizer tongfengOpenApiCustomizer() {
		return openApi -> {
			if (openApi.getPaths() == null) {
				return;
			}
			openApi.getPaths().forEach((path, pathItem) -> {
				if (pathItem.readOperations() == null) {
					return;
				}
				pathItem.readOperations().forEach(operation -> {
					if (operation.getSummary() == null || operation.getSummary().isBlank()) {
						operation.setSummary(buildFallbackSummary(path));
					}
				});
			});
		};
	}

	private String extractPath(HandlerMethod handlerMethod) {
		var requestMapping = handlerMethod.getMethodAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
		if (requestMapping != null && requestMapping.value().length > 0) {
			return requestMapping.value()[0];
		}
		for (var annotation : handlerMethod.getMethod().getAnnotations()) {
			var type = annotation.annotationType();
			try {
				var valueMethod = type.getMethod("value");
				Object raw = valueMethod.invoke(annotation);
				if (raw instanceof String[] values && values.length > 0) {
					return values[0];
				}
			} catch (ReflectiveOperationException ignored) {
				// ignore
			}
		}
		return "";
	}

	private void applyTag(io.swagger.v3.oas.models.Operation operation, String path) {
		String tagName;
		if (path.startsWith("/api/public") || path.startsWith("/api/openapi")) {
			tagName = "系统元信息";
		} else if (path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/privacy")) {
			tagName = "认证与隐私";
		} else if (path.startsWith("/api/v1/family")) {
			tagName = "家属协同";
		} else if (path.startsWith("/api/v1/meals")
				|| path.startsWith("/api/v1/lab-reports")
				|| path.startsWith("/api/v1/knowledge")
				|| path.startsWith("/api/v1/analysis")) {
			tagName = "饮食与化验";
		} else {
			tagName = "健康档案";
		}
		operation.setTags(List.of(tagName));
	}

	private void applySecurity(io.swagger.v3.oas.models.Operation operation, String path) {
		if (isPublicPath(path)) {
			operation.setSecurity(new ArrayList<>());
			return;
		}
		operation.setSecurity(List.of(new SecurityRequirement().addList(SECURITY_SCHEME_NAME)));
	}

	private void applyDefaultResponses(io.swagger.v3.oas.models.Operation operation, String path) {
		if (operation.getResponses() == null) {
			operation.setResponses(new io.swagger.v3.oas.models.responses.ApiResponses());
		}
		operation.getResponses().addApiResponse("400", new ApiResponse().$ref("#/components/responses/ValidationError"));
		operation.getResponses().addApiResponse("500", new ApiResponse().$ref("#/components/responses/InternalServerError"));
		if (!isPublicPath(path)) {
			operation.getResponses().addApiResponse("401", new ApiResponse().$ref("#/components/responses/UnauthorizedError"));
			operation.getResponses().addApiResponse("403", new ApiResponse().$ref("#/components/responses/ForbiddenError"));
		}
		if (path.contains("verification-codes") || path.contains("auth/login")) {
			operation.getResponses().addApiResponse("429", new ApiResponse().$ref("#/components/responses/TooManyRequestsError"));
		}
	}

	private void applyEndpointExamples(io.swagger.v3.oas.models.Operation operation, String path) {
		switch (path) {
			case "/api/v1/auth/register" -> {
				applyJsonRequestExample(operation, "register", "#/components/examples/registerRequestExample");
				applyJsonSuccessResponseExample(operation, "#/components/examples/authTokenResponseExample");
			}
			case "/api/v1/auth/login" -> {
				applyJsonRequestExample(operation, "login", "#/components/examples/loginRequestExample");
				applyJsonSuccessResponseExample(operation, "#/components/examples/authTokenResponseExample");
			}
			case "/api/v1/auth/verification-codes/request" -> {
				applyJsonRequestExample(operation, "verificationRequest", "#/components/examples/verificationCodeRequestExample");
				applyJsonSuccessResponseExample(operation, "#/components/examples/verificationChallengeResponseExample");
			}
			case "/api/v1/meals/analyze" -> {
				applyMultipartRequestExample(operation, "mealAnalyze", "#/components/examples/mealAnalyzeMultipartExample");
				applyJsonSuccessResponseExample(operation, "#/components/examples/mealAnalyzeFallbackResponseExample");
			}
			case "/api/v1/lab-reports/analyze" -> {
				applyMultipartRequestExample(operation, "labAnalyze", "#/components/examples/labAnalyzeMultipartExample");
				applyJsonSuccessResponseExample(operation, "#/components/examples/labAnalyzeFallbackResponseExample");
			}
			case "/api/v1/lab-reports/{reportId}/review" -> applyJsonSuccessResponseExample(operation, "#/components/examples/labManualPendingReviewResponseExample");
			case "/api/v1/lab-reports/{reportId}/manual-confirmation" -> {
				applyJsonRequestExample(operation, "labManualConfirmation", "#/components/examples/labManualConfirmationRequestExample");
				applyJsonSuccessResponseExample(operation, "#/components/examples/labManualPendingReviewResponseExample");
			}
			case "/api/v1/knowledge/ask" -> {
				applyJsonRequestExample(operation, "knowledgeAsk", "#/components/examples/knowledgeAskRequestExample");
			}
			case "/api/public/error-codes" -> applyJsonSuccessResponseExample(operation, "#/components/examples/errorCodeCatalogResponseExample");
			default -> {
			}
		}
	}

	private void applyJsonRequestExample(io.swagger.v3.oas.models.Operation operation, String exampleKey, String exampleRef) {
		RequestBody requestBody = ensureRequestBody(operation);
		Content content = ensureContent(requestBody.getContent());
		requestBody.setContent(content);
		MediaType mediaType = ensureMediaType(content, JSON_MEDIA_TYPE);
		mediaType.addExamples(exampleKey, new Example().$ref(exampleRef));
		requestBody.setRequired(Boolean.TRUE.equals(requestBody.getRequired()) ? true : requestBody.getRequired());
	}

	private void applyMultipartRequestExample(io.swagger.v3.oas.models.Operation operation, String exampleKey, String exampleRef) {
		RequestBody requestBody = ensureRequestBody(operation);
		Content content = ensureContent(requestBody.getContent());
		requestBody.setContent(content);
		MediaType mediaType = ensureMediaType(content, MULTIPART_MEDIA_TYPE);
		mediaType.addExamples(exampleKey, new Example().$ref(exampleRef));
	}

	private void applyJsonSuccessResponseExample(io.swagger.v3.oas.models.Operation operation, String exampleRef) {
		if (operation.getResponses() == null || operation.getResponses().get("200") == null) {
			return;
		}
		ApiResponse response = operation.getResponses().get("200");
		Content content = ensureContent(response.getContent());
		response.setContent(content);
		MediaType mediaType = ensureMediaType(content, JSON_MEDIA_TYPE);
		mediaType.addExamples("success", new Example().$ref(exampleRef));
	}

	private RequestBody ensureRequestBody(io.swagger.v3.oas.models.Operation operation) {
		if (operation.getRequestBody() == null) {
			operation.setRequestBody(new RequestBody());
		}
		return operation.getRequestBody();
	}

	private Content ensureContent(Content content) {
		return content == null ? new Content() : content;
	}

	private MediaType ensureMediaType(Content content, String mediaTypeKey) {
		MediaType mediaType = content.get(mediaTypeKey);
		if (mediaType == null) {
			mediaType = new MediaType();
			content.addMediaType(mediaTypeKey, mediaType);
		}
		return mediaType;
	}

	private boolean isPublicPath(String path) {
		return path.startsWith("/api/public")
				|| path.startsWith("/api/openapi")
				|| path.startsWith("/api/v1/auth/mock-login")
				|| path.startsWith("/api/v1/auth/register")
				|| path.startsWith("/api/v1/auth/login")
				|| path.startsWith("/api/v1/auth/verification-codes/request")
				|| path.startsWith("/api/v1/auth/password-reset/confirm");
	}

	private String buildFallbackSummary(String path) {
		if (path.startsWith("/api/v1/auth/mock-login")) {
			return "开发环境模拟登录";
		}
		if (path.startsWith("/api/v1/auth/register")) {
			return "注册正式账号";
		}
		if (path.startsWith("/api/v1/auth/login")) {
			return "账号密码登录";
		}
		if (path.startsWith("/api/v1/lab-reports/analyze")) {
			return "上传化验单并解析";
		}
		if (path.startsWith("/api/v1/lab-reports/") && path.endsWith("/review")) {
			return "查看化验单复盘";
		}
		if (path.startsWith("/api/v1/meals/analyze")) {
			return "上传餐盘并分析";
		}
		if (path.startsWith("/api/public/error-codes")) {
			return "获取后端错误码字典";
		}
		return path;
	}

	private Example jsonExample(String summary, String jsonValue) {
		Example example = new Example().summary(summary);
		try {
			example.setValue(objectMapper.readValue(jsonValue, Object.class));
		} catch (JsonProcessingException ex) {
			example.setValue(jsonValue);
		}
		return example;
	}

	private ApiResponse buildErrorResponse(String description, String exampleRef) {
		return new ApiResponse()
				.description(description)
				.content(new Content().addMediaType(
						JSON_MEDIA_TYPE,
						new MediaType().addExamples("default", new Example().$ref(exampleRef))
				));
	}
}

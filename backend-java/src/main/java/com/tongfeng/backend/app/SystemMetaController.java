package com.tongfeng.backend.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemMetaController {

	@Operation(
			summary = "获取后端错误码字典",
			description = "返回当前后端统一错误码目录，可作为前后端联调、日志排查和 OpenAPI 之外的稳定错误语义来源。"
	)
	@SecurityRequirements
	@GetMapping("/api/public/error-codes")
	public ApiResponse<AppContracts.ErrorCodeCatalogResponse> getErrorCodeCatalog() {
		List<AppContracts.ErrorCodeItemResponse> items = AppErrorCode.catalog().stream()
				.map(item -> new AppContracts.ErrorCodeItemResponse(
						item.code(),
						item.httpStatus().value(),
						item.httpStatus().name(),
						item.category(),
						item.retryable(),
						item.defaultMessage()
				))
				.toList();
		return ApiResponse.success(new AppContracts.ErrorCodeCatalogResponse(
				"2026-05",
				Instant.now(),
				items
		));
	}
}

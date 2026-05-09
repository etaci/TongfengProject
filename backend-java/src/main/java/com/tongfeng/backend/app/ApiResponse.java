package com.tongfeng.backend.app;

import java.time.Instant;

public record ApiResponse<T>(
		boolean success,
		String code,
		String message,
		T data,
		Instant timestamp,
		String traceId,
		String path
) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(
				true,
				AppErrorCode.OK.code(),
				AppErrorCode.OK.defaultMessage(),
				data,
				Instant.now(),
				RequestTraceContext.traceId(),
				RequestTraceContext.path()
		);
	}

	public static <T> ApiResponse<T> failure(String code, String message) {
		return new ApiResponse<>(
				false,
				code,
				message,
				null,
				Instant.now(),
				RequestTraceContext.traceId(),
				RequestTraceContext.path()
		);
	}

	public static <T> ApiResponse<T> failure(AppErrorCode errorCode, String message) {
		return failure(errorCode.code(), message == null || message.isBlank() ? errorCode.defaultMessage() : message);
	}
}

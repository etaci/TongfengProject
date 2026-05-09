package com.tongfeng.backend.app;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

	private final String code;
	private final HttpStatus httpStatus;

	public BusinessException(String code, String message) {
		this(resolveCode(code), resolveMessage(code, message), resolveHttpStatus(code));
	}

	public BusinessException(AppErrorCode errorCode, String message) {
		this(errorCode.code(), message == null || message.isBlank() ? errorCode.defaultMessage() : message, errorCode.httpStatus());
	}

	public BusinessException(String code, String message, HttpStatus httpStatus) {
		super(message);
		this.code = code;
		this.httpStatus = httpStatus;
	}

	private static String resolveCode(String code) {
		return AppErrorCode.fromCode(code).map(AppErrorCode::code).orElse(code);
	}

	private static String resolveMessage(String code, String message) {
		if (message != null && !message.isBlank()) {
			return message;
		}
		return AppErrorCode.fromCode(code).map(AppErrorCode::defaultMessage).orElse("");
	}

	private static HttpStatus resolveHttpStatus(String code) {
		return AppErrorCode.fromCode(code)
				.map(AppErrorCode::httpStatus)
				.orElse(HttpStatus.BAD_REQUEST);
	}

	public String getCode() {
		return code;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
}

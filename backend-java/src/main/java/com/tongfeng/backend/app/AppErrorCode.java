package com.tongfeng.backend.app;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;

public enum AppErrorCode {

	OK("OK", HttpStatus.OK, "SYSTEM", false, "success"),

	UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "AUTH", false, "未授权访问"),
	FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "AUTH", false, "无权访问"),
	VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "VALIDATION", false, "请求参数校验失败"),
	MISSING_PART("MISSING_PART", HttpStatus.BAD_REQUEST, "VALIDATION", false, "缺少必要请求字段"),
	INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "SYSTEM", true, "服务器内部错误"),
	DEPENDENCY_UNAVAILABLE("DEPENDENCY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY", true, "依赖服务暂时不可用"),

	ACCOUNT_EXISTS("ACCOUNT_EXISTS", HttpStatus.CONFLICT, "ACCOUNT", false, "账号已存在"),
	ACCOUNT_LOGIN_LOCKED("ACCOUNT_LOGIN_LOCKED", HttpStatus.TOO_MANY_REQUESTS, "ACCOUNT", true, "登录失败次数过多，账号已被暂时锁定"),
	ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", HttpStatus.BAD_REQUEST, "ACCOUNT", false, "账号不存在或已停用"),
	ACCOUNT_REQUIRED("ACCOUNT_REQUIRED", HttpStatus.BAD_REQUEST, "ACCOUNT", false, "账号不能为空"),
	ACCOUNT_TYPE_INVALID("ACCOUNT_TYPE_INVALID", HttpStatus.BAD_REQUEST, "ACCOUNT", false, "账号类型不支持"),
	ACCOUNT_TYPE_MISMATCH("ACCOUNT_TYPE_MISMATCH", HttpStatus.BAD_REQUEST, "ACCOUNT", false, "账号类型不匹配"),
	ACCOUNT_TYPE_REQUIRED("ACCOUNT_TYPE_REQUIRED", HttpStatus.BAD_REQUEST, "ACCOUNT", false, "账号类型不能为空"),
	ACCOUNT_VERIFICATION_NOT_SUPPORTED("ACCOUNT_VERIFICATION_NOT_SUPPORTED", HttpStatus.BAD_REQUEST, "ACCOUNT", false, "当前登录方式不支持账号验证"),

	AUTH_HASH_ERROR("AUTH_HASH_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "AUTH", true, "账号安全指纹生成失败"),
	INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.BAD_REQUEST, "AUTH", false, "账号或密码不正确"),
	INVALID_CURRENT_PASSWORD("INVALID_CURRENT_PASSWORD", HttpStatus.BAD_REQUEST, "AUTH", false, "当前密码不正确"),
	PASSWORD_CHANGE_NOT_SUPPORTED("PASSWORD_CHANGE_NOT_SUPPORTED", HttpStatus.BAD_REQUEST, "AUTH", false, "当前登录方式不支持修改密码"),
	PASSWORD_CONFIRM_MISMATCH("PASSWORD_CONFIRM_MISMATCH", HttpStatus.BAD_REQUEST, "AUTH", false, "两次输入的密码不一致"),
	PASSWORD_HASH_ERROR("PASSWORD_HASH_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "AUTH", true, "密码摘要生成失败"),
	PASSWORD_TOO_SHORT("PASSWORD_TOO_SHORT", HttpStatus.BAD_REQUEST, "AUTH", false, "密码至少需要 8 位"),
	PASSWORD_UNCHANGED("PASSWORD_UNCHANGED", HttpStatus.BAD_REQUEST, "AUTH", false, "新密码不能与当前密码相同"),
	SESSION_NOT_FOUND("SESSION_NOT_FOUND", HttpStatus.NOT_FOUND, "AUTH", false, "目标会话不存在或已失效"),
	USE_LOGOUT_FOR_CURRENT_SESSION("USE_LOGOUT_FOR_CURRENT_SESSION", HttpStatus.CONFLICT, "AUTH", false, "当前设备请直接使用退出登录"),

	PRIVACY_CONSENT_NOT_FOUND("PRIVACY_CONSENT_NOT_FOUND", HttpStatus.NOT_FOUND, "PRIVACY", false, "隐私授权记录不存在"),
	PRIVACY_CONSENT_REQUIRED("PRIVACY_CONSENT_REQUIRED", HttpStatus.FORBIDDEN, "PRIVACY", false, "请先完成隐私政策和服务条款授权"),

	EMAIL_INVALID("EMAIL_INVALID", HttpStatus.BAD_REQUEST, "ACCOUNT", false, "请输入有效的邮箱账号"),
	PHONE_INVALID("PHONE_INVALID", HttpStatus.BAD_REQUEST, "ACCOUNT", false, "请输入有效的手机号"),
	USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "ACCOUNT", false, "用户不存在"),
	PROFILE_NOT_FOUND("PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND, "ACCOUNT", false, "用户档案不存在"),

	EMPTY_FILE("EMPTY_FILE", HttpStatus.BAD_REQUEST, "FILE", false, "上传文件不能为空"),
	FILE_NOT_FOUND("FILE_NOT_FOUND", HttpStatus.NOT_FOUND, "FILE", false, "文件不存在"),
	FILE_READ_ERROR("FILE_READ_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "FILE", true, "读取上传文件失败"),
	FILE_SAVE_ERROR("FILE_SAVE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "FILE", true, "保存文件失败"),
	JSON_ENCODE_ERROR("JSON_ENCODE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "SYSTEM", true, "JSON 编码失败"),
	JSON_DECODE_ERROR("JSON_DECODE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "SYSTEM", true, "JSON 解码失败"),

	INVITE_CODE_REQUIRED("INVITE_CODE_REQUIRED", HttpStatus.BAD_REQUEST, "FAMILY", false, "邀请编码不能为空"),
	INVITE_NOT_FOUND("INVITE_NOT_FOUND", HttpStatus.NOT_FOUND, "FAMILY", false, "邀请不存在或已失效"),
	INVITE_SELF_ACCEPT("INVITE_SELF_ACCEPT", HttpStatus.CONFLICT, "FAMILY", false, "不能接受自己发起的家属邀请"),
	INVITE_EXPIRED("INVITE_EXPIRED", HttpStatus.CONFLICT, "FAMILY", false, "邀请已过期"),
	INVITE_UNAVAILABLE("INVITE_UNAVAILABLE", HttpStatus.CONFLICT, "FAMILY", false, "邀请当前不可用"),
	BINDING_EXISTS("BINDING_EXISTS", HttpStatus.CONFLICT, "FAMILY", false, "绑定关系已存在"),
	BINDING_NOT_FOUND("BINDING_NOT_FOUND", HttpStatus.NOT_FOUND, "FAMILY", false, "绑定关系不存在"),
	BINDING_INACTIVE("BINDING_INACTIVE", HttpStatus.CONFLICT, "FAMILY", false, "当前绑定关系不可操作"),
	WEEKLY_REPORT_DISABLED("WEEKLY_REPORT_DISABLED", HttpStatus.FORBIDDEN, "FAMILY", false, "当前患者未开放周报共享"),
	FAMILY_TASK_NOT_FOUND("FAMILY_TASK_NOT_FOUND", HttpStatus.NOT_FOUND, "FAMILY", false, "家属代办不存在"),
	FAMILY_TASK_COMPLETED("FAMILY_TASK_COMPLETED", HttpStatus.CONFLICT, "FAMILY", false, "家属代办已完成"),
	FAMILY_TASK_PERMISSION_DENIED("FAMILY_TASK_PERMISSION_DENIED", HttpStatus.FORBIDDEN, "FAMILY", false, "当前绑定关系未开放共同照护权限"),
	FAMILY_PERMISSION_INVALID("FAMILY_PERMISSION_INVALID", HttpStatus.BAD_REQUEST, "FAMILY", false, "家属权限不支持"),

	FEATURE_DISABLED("FEATURE_DISABLED", HttpStatus.FORBIDDEN, "FEATURE", false, "当前功能未开放"),

	FLARE_NOT_FOUND("FLARE_NOT_FOUND", HttpStatus.NOT_FOUND, "ANALYSIS", false, "暂无发作记录"),
	LAB_REPORT_NOT_FOUND("LAB_REPORT_NOT_FOUND", HttpStatus.NOT_FOUND, "LAB", false, "化验单不存在"),
	LAB_REPORT_ALREADY_READY("LAB_REPORT_ALREADY_READY", HttpStatus.CONFLICT, "LAB", false, "当前化验单已完成 OCR 解析"),

	MEDICATION_PLAN_REQUIRED("MEDICATION_PLAN_REQUIRED", HttpStatus.CONFLICT, "MEDICATION", false, "请先维护当前用药计划"),
	MEDICATION_NOT_FOUND("MEDICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "MEDICATION", false, "当前用药计划中未找到该药物"),
	MEDICATION_PERIOD_INVALID("MEDICATION_PERIOD_INVALID", HttpStatus.BAD_REQUEST, "MEDICATION", false, "服药时段不支持或与计划不匹配"),
	MEDICATION_STATUS_INVALID("MEDICATION_STATUS_INVALID", HttpStatus.BAD_REQUEST, "MEDICATION", false, "用药状态不支持"),

	RECORD_NOT_FOUND("RECORD_NOT_FOUND", HttpStatus.NOT_FOUND, "RECORD", false, "记录不存在"),
	RECORD_AUDIT_NOT_FOUND("RECORD_AUDIT_NOT_FOUND", HttpStatus.NOT_FOUND, "RECORD", false, "审计记录不存在"),
	RECORD_CURSOR_INVALID("RECORD_CURSOR_INVALID", HttpStatus.BAD_REQUEST, "RECORD", false, "记录中心游标格式不正确"),
	RECORD_TYPE_UNSUPPORTED("RECORD_TYPE_UNSUPPORTED", HttpStatus.BAD_REQUEST, "RECORD", false, "记录类型暂不支持"),
	RECORD_UPDATE_INVALID("RECORD_UPDATE_INVALID", HttpStatus.BAD_REQUEST, "RECORD", false, "记录更正请求不合法"),
	RECORD_UPDATE_NO_CHANGES("RECORD_UPDATE_NO_CHANGES", HttpStatus.CONFLICT, "RECORD", false, "记录内容没有实际变化"),
	RECORD_RESTORE_INVALID("RECORD_RESTORE_INVALID", HttpStatus.BAD_REQUEST, "RECORD", false, "记录恢复请求不合法"),
	RECORD_RESTORE_NO_CHANGES("RECORD_RESTORE_NO_CHANGES", HttpStatus.CONFLICT, "RECORD", false, "恢复后记录没有变化"),
	RECORD_RESTORE_UNSUPPORTED("RECORD_RESTORE_UNSUPPORTED", HttpStatus.BAD_REQUEST, "RECORD", false, "当前审计记录不支持恢复"),

	VERIFICATION_PURPOSE_REQUIRED("VERIFICATION_PURPOSE_REQUIRED", HttpStatus.BAD_REQUEST, "VERIFICATION", false, "验证码用途不能为空"),
	VERIFICATION_PURPOSE_INVALID("VERIFICATION_PURPOSE_INVALID", HttpStatus.BAD_REQUEST, "VERIFICATION", false, "验证码用途不支持"),
	VERIFICATION_CODE_COOLDOWN("VERIFICATION_CODE_COOLDOWN", HttpStatus.TOO_MANY_REQUESTS, "VERIFICATION", true, "验证码请求过于频繁"),
	VERIFICATION_CODE_NOT_FOUND("VERIFICATION_CODE_NOT_FOUND", HttpStatus.BAD_REQUEST, "VERIFICATION", false, "当前没有可用验证码"),
	VERIFICATION_CODE_EXPIRED("VERIFICATION_CODE_EXPIRED", HttpStatus.BAD_REQUEST, "VERIFICATION", false, "验证码已过期"),
	VERIFICATION_CODE_INVALID("VERIFICATION_CODE_INVALID", HttpStatus.BAD_REQUEST, "VERIFICATION", false, "验证码不正确"),
	VERIFICATION_DELIVERY_UNAVAILABLE("VERIFICATION_DELIVERY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "VERIFICATION", true, "验证码投递服务暂不可用");

	private final String code;
	private final HttpStatus httpStatus;
	private final String category;
	private final boolean retryable;
	private final String defaultMessage;

	AppErrorCode(String code, HttpStatus httpStatus, String category, boolean retryable, String defaultMessage) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.category = category;
		this.retryable = retryable;
		this.defaultMessage = defaultMessage;
	}

	public String code() {
		return code;
	}

	public HttpStatus httpStatus() {
		return httpStatus;
	}

	public String category() {
		return category;
	}

	public boolean retryable() {
		return retryable;
	}

	public String defaultMessage() {
		return defaultMessage;
	}

	public static Optional<AppErrorCode> fromCode(String code) {
		if (code == null || code.isBlank()) {
			return Optional.empty();
		}
		return Arrays.stream(values())
				.filter(item -> item.code.equals(code))
				.findFirst();
	}

	public static List<AppErrorCode> catalog() {
		return Arrays.stream(values())
				.filter(item -> item != OK)
				.toList();
	}
}

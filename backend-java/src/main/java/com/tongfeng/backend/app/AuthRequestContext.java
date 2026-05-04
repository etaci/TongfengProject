package com.tongfeng.backend.app;

public record AuthRequestContext(
		String clientIp,
		String userAgent,
		String deviceFingerprint,
		String deviceLabel
) {
}

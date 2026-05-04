package com.tongfeng.backend.app;

import java.time.Instant;
import java.util.List;

public record UserSession(
		String sessionCode,
		String userId,
		String nickname,
		String authMode,
		String accountType,
		String accountIdentifier,
		String deviceLabel,
		String clientIpMasked,
		String loginRiskLevel,
		List<String> securityNotices,
		boolean accountVerified,
		boolean privacyConsentCompleted,
		Instant createdAt,
		Instant lastSeenAt,
		String token,
		Instant expiresAt
) {
}

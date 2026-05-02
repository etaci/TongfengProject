package com.tongfeng.backend.app;

import java.time.Instant;

public record UserSession(
		String sessionCode,
		String userId,
		String nickname,
		String authMode,
		String accountType,
		String accountIdentifier,
		boolean privacyConsentCompleted,
		Instant createdAt,
		Instant lastSeenAt,
		String token,
		Instant expiresAt
) {
}

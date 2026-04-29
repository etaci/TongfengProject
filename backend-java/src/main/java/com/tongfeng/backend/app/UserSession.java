package com.tongfeng.backend.app;

import java.time.Instant;

public record UserSession(
		String userId,
		String nickname,
		String token,
		Instant expiresAt
) {
}

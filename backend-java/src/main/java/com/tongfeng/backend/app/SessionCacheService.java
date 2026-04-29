package com.tongfeng.backend.app;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SessionCacheService {

	private final AppProperties appProperties;
	private final JsonCodec jsonCodec;
	private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

	public SessionCacheService(
			AppProperties appProperties,
			JsonCodec jsonCodec,
			ObjectProvider<StringRedisTemplate> redisTemplateProvider
	) {
		this.appProperties = appProperties;
		this.jsonCodec = jsonCodec;
		this.redisTemplateProvider = redisTemplateProvider;
	}

	public Optional<UserSession> get(String token) {
		if (!appProperties.isRedisEnabled()) {
			return Optional.empty();
		}
		try {
			StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
			if (redisTemplate == null) {
				return Optional.empty();
			}
			String value = redisTemplate.opsForValue().get(cacheKey(token));
			if (value == null) {
				return Optional.empty();
			}
			return Optional.of(jsonCodec.fromJson(value, UserSession.class));
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	public void put(UserSession session) {
		if (!appProperties.isRedisEnabled()) {
			return;
		}
		try {
			StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
			if (redisTemplate == null) {
				return;
			}
			Duration ttl = Duration.between(Instant.now(), session.expiresAt());
			if (ttl.isNegative() || ttl.isZero()) {
				return;
			}
			redisTemplate.opsForValue().set(cacheKey(session.token()), jsonCodec.toJson(session), ttl);
		} catch (Exception ignored) {
		}
	}

	public void evict(String token) {
		if (!appProperties.isRedisEnabled()) {
			return;
		}
		try {
			StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
			if (redisTemplate != null) {
				redisTemplate.delete(cacheKey(token));
			}
		} catch (Exception ignored) {
		}
	}

	private String cacheKey(String token) {
		return appProperties.getSessionCachePrefix() + token;
	}
}

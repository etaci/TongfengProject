package com.tongfeng.backend.app;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PasswordHashService {

	private static final int ITERATIONS = 65_536;
	private static final int KEY_LENGTH = 256;
	private static final int SALT_BYTES = 16;

	public record HashedPassword(String salt, String hash) {
	}

	public HashedPassword hash(String rawPassword) {
		byte[] salt = new byte[SALT_BYTES];
		try {
			SecureRandom.getInstanceStrong().nextBytes(salt);
		} catch (NoSuchAlgorithmException ex) {
			new SecureRandom().nextBytes(salt);
		}
		return new HashedPassword(encode(salt), derive(rawPassword, salt));
	}

	public boolean matches(String rawPassword, String salt, String expectedHash) {
		if (rawPassword == null || salt == null || expectedHash == null) {
			return false;
		}
		return expectedHash.equals(derive(rawPassword, Base64.getDecoder().decode(salt)));
	}

	private String derive(String rawPassword, byte[] salt) {
		try {
			PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
			byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
			return encode(hash);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
			throw new BusinessException("PASSWORD_HASH_ERROR", "密码摘要生成失败");
		}
	}

	private String encode(byte[] value) {
		return Base64.getEncoder().encodeToString(value);
	}
}

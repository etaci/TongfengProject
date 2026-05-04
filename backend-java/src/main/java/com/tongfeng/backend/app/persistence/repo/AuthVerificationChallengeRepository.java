package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.AuthVerificationChallengeEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthVerificationChallengeRepository extends JpaRepository<AuthVerificationChallengeEntity, Long> {

	List<AuthVerificationChallengeEntity> findByPrincipalValueAndPurposeAndStatus(String principalValue, String purpose, String status);

	Optional<AuthVerificationChallengeEntity> findFirstByPrincipalValueAndPurposeAndStatusOrderByCreatedAtDesc(
			String principalValue,
			String purpose,
			String status
	);

	Optional<AuthVerificationChallengeEntity> findFirstByPrincipalValueAndPurposeOrderByCreatedAtDesc(
			String principalValue,
			String purpose
	);

	long countByPrincipalValueAndPurposeAndCreatedAtAfter(String principalValue, String purpose, Instant createdAt);

	List<AuthVerificationChallengeEntity> findByStatusAndExpiresAtBefore(String status, Instant expiresAt);
}

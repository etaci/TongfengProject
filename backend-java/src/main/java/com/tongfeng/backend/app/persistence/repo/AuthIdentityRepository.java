package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.AuthIdentityEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentityEntity, Long> {

	Optional<AuthIdentityEntity> findByPrincipalValueAndStatus(String principalValue, String status);

	Optional<AuthIdentityEntity> findByUserCodeAndAccountTypeAndPrincipalValueAndStatus(
			String userCode,
			String accountType,
			String principalValue,
			String status
	);
}

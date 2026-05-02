package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.AuthSessionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, Long> {

	Optional<AuthSessionEntity> findByToken(String token);

	Optional<AuthSessionEntity> findBySessionCode(String sessionCode);

	Optional<AuthSessionEntity> findBySessionCodeAndUserCode(String sessionCode, String userCode);

	List<AuthSessionEntity> findByUserCodeOrderByLastSeenAtDescCreatedAtDesc(String userCode);

	void deleteByToken(String token);
}

package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.AuthSessionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, Long> {

	Optional<AuthSessionEntity> findByToken(String token);
}

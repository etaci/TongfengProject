package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.UserProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

	Optional<UserProfileEntity> findByUserCode(String userCode);
}

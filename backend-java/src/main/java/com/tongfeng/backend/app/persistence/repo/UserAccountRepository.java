package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.UserAccountEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {

	Optional<UserAccountEntity> findByUserCode(String userCode);
}

package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.GrowthProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthProfileRepository extends JpaRepository<GrowthProfileEntity, Long> {

	Optional<GrowthProfileEntity> findByUserCode(String userCode);
}

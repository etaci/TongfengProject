package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.MedicationPlanEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationPlanRepository extends JpaRepository<MedicationPlanEntity, Long> {

	Optional<MedicationPlanEntity> findByUserCode(String userCode);
}

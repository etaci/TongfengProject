package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.ClinicalDecisionAuditEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalDecisionAuditRepository extends JpaRepository<ClinicalDecisionAuditEntity, Long> {

	Optional<ClinicalDecisionAuditEntity> findByDecisionCodeAndUserCode(String decisionCode, String userCode);

	Optional<ClinicalDecisionAuditEntity> findFirstByUserCodeAndDecisionTypeOrderByGeneratedAtDesc(
			String userCode,
			String decisionType
	);

	List<ClinicalDecisionAuditEntity> findByUserCodeAndDecisionTypeOrderByGeneratedAtDesc(
			String userCode,
			String decisionType
	);
}

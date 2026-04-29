package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.HealthRecordAuditEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthRecordAuditRepository extends JpaRepository<HealthRecordAuditEntity, Long> {

	Optional<HealthRecordAuditEntity> findByAuditCode(String auditCode);

	List<HealthRecordAuditEntity> findByUserCodeAndRecordTypeAndRecordIdOrderByOperatedAtDesc(
			String userCode,
			String recordType,
			String recordId
	);
}

package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.AccessAuditEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessAuditRepository extends JpaRepository<AccessAuditEntity, Long> {

	List<AccessAuditEntity> findByActorUserCodeOrderByOperatedAtDesc(String actorUserCode);

	List<AccessAuditEntity> findByPatientUserCodeOrderByOperatedAtDesc(String patientUserCode);
}

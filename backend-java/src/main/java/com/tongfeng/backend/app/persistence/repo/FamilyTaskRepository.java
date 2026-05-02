package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.FamilyTaskEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyTaskRepository extends JpaRepository<FamilyTaskEntity, Long> {

	List<FamilyTaskEntity> findByPatientUserCodeOrderByCreatedAtDesc(String patientUserCode);

	List<FamilyTaskEntity> findByCaregiverUserCodeOrderByCreatedAtDesc(String caregiverUserCode);

	Optional<FamilyTaskEntity> findByTaskCode(String taskCode);
}

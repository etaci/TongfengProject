package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.FamilyBindingEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyBindingRepository extends JpaRepository<FamilyBindingEntity, Long> {

	List<FamilyBindingEntity> findByPatientUserCodeOrderByCreatedAtDesc(String patientUserCode);

	List<FamilyBindingEntity> findByCaregiverUserCodeOrderByCreatedAtDesc(String caregiverUserCode);

	Optional<FamilyBindingEntity> findByBindingCode(String bindingCode);

	Optional<FamilyBindingEntity> findByPatientUserCodeAndCaregiverUserCodeAndStatus(
			String patientUserCode,
			String caregiverUserCode,
			String status
	);
}

package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.MedicationCheckinEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationCheckinRepository extends JpaRepository<MedicationCheckinEntity, Long> {

	Optional<MedicationCheckinEntity> findByUserCodeAndCheckinDateAndMedicationNameIgnoreCaseAndScheduledPeriod(
			String userCode,
			LocalDate checkinDate,
			String medicationName,
			String scheduledPeriod
	);

	List<MedicationCheckinEntity> findByUserCodeAndCheckinDateGreaterThanEqualOrderByCheckinDateDescCheckinAtDesc(
			String userCode,
			LocalDate startDate
	);
}

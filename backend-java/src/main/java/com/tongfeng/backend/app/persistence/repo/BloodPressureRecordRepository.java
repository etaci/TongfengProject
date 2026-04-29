package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.BloodPressureRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodPressureRecordRepository extends JpaRepository<BloodPressureRecordEntity, Long> {

	List<BloodPressureRecordEntity> findByUserCodeOrderByMeasuredAtDesc(String userCode);

	Optional<BloodPressureRecordEntity> findByRecordCode(String recordCode);
}

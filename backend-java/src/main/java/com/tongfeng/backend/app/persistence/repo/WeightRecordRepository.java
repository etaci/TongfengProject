package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.WeightRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightRecordRepository extends JpaRepository<WeightRecordEntity, Long> {

	List<WeightRecordEntity> findByUserCodeOrderByMeasuredAtDesc(String userCode);

	Optional<WeightRecordEntity> findByRecordCode(String recordCode);
}

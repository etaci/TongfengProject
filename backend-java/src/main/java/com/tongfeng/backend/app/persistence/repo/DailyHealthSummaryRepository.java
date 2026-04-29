package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.DailyHealthSummaryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyHealthSummaryRepository extends JpaRepository<DailyHealthSummaryEntity, Long> {

	Optional<DailyHealthSummaryEntity> findByUserCodeAndSummaryDate(String userCode, LocalDate summaryDate);

	List<DailyHealthSummaryEntity> findByUserCodeOrderBySummaryDateDesc(String userCode);
}

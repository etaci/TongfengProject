package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.WeatherDailySnapshotEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherDailySnapshotRepository extends JpaRepository<WeatherDailySnapshotEntity, Long> {

	Optional<WeatherDailySnapshotEntity> findByUserCodeAndSummaryDate(String userCode, LocalDate summaryDate);

	Optional<WeatherDailySnapshotEntity> findFirstByUserCodeOrderBySummaryDateDesc(String userCode);
}

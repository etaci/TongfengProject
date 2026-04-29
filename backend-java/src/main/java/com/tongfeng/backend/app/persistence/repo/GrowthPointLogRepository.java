package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.GrowthPointLogEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthPointLogRepository extends JpaRepository<GrowthPointLogEntity, Long> {

	List<GrowthPointLogEntity> findTop50ByUserCodeOrderByCreatedAtDesc(String userCode);

	List<GrowthPointLogEntity> findByUserCodeAndAwardedDateOrderByCreatedAtDesc(String userCode, LocalDate awardedDate);

	List<GrowthPointLogEntity> findByUserCodeOrderByCreatedAtDesc(String userCode);

	Optional<GrowthPointLogEntity> findByUserCodeAndDedupKey(String userCode, String dedupKey);
}

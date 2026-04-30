package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.MvpUsageEventEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MvpUsageEventRepository extends JpaRepository<MvpUsageEventEntity, Long> {

	List<MvpUsageEventEntity> findByEventDateGreaterThanEqualOrderByCreatedAtDesc(LocalDate eventDate);
}

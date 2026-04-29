package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.HydrationRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HydrationRecordRepository extends JpaRepository<HydrationRecordEntity, Long> {

	List<HydrationRecordEntity> findByUserCodeOrderByCheckedAtDesc(String userCode);

	Optional<HydrationRecordEntity> findByRecordCode(String recordCode);
}

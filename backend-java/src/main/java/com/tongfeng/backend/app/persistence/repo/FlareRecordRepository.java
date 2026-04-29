package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.FlareRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlareRecordRepository extends JpaRepository<FlareRecordEntity, Long> {

	List<FlareRecordEntity> findByUserCodeOrderByStartedAtDesc(String userCode);

	Optional<FlareRecordEntity> findByRecordCode(String recordCode);
}

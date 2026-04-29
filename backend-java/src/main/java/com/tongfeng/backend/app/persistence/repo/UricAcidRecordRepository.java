package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.UricAcidRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UricAcidRecordRepository extends JpaRepository<UricAcidRecordEntity, Long> {

	List<UricAcidRecordEntity> findByUserCodeOrderByMeasuredAtDesc(String userCode);

	Optional<UricAcidRecordEntity> findByRecordCode(String recordCode);
}

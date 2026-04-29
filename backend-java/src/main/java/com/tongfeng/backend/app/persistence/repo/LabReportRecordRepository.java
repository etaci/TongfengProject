package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.LabReportRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabReportRecordRepository extends JpaRepository<LabReportRecordEntity, Long> {

	List<LabReportRecordEntity> findByUserCodeOrderByReportDateDesc(String userCode);
}

package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.MealRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRecordRepository extends JpaRepository<MealRecordEntity, Long> {

	List<MealRecordEntity> findByUserCodeOrderByTakenAtDesc(String userCode);
}

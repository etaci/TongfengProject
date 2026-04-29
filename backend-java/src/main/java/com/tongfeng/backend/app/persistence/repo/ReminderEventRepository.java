package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.ReminderEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ReminderEventRepository extends JpaRepository<ReminderEventEntity, Long> {

	List<ReminderEventEntity> findByUserCodeAndStatusOrderByTriggerAtDesc(String userCode, String status);

	@Transactional
	void deleteByUserCodeAndStatus(String userCode, String status);
}

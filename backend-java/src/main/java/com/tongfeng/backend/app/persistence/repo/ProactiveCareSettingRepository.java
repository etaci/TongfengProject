package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.ProactiveCareSettingEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProactiveCareSettingRepository extends JpaRepository<ProactiveCareSettingEntity, Long> {

	Optional<ProactiveCareSettingEntity> findByUserCode(String userCode);

	List<ProactiveCareSettingEntity> findByWeatherAlertsEnabledTrue();
}

package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.DeviceSyncEventEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceSyncEventRepository extends JpaRepository<DeviceSyncEventEntity, Long> {

	List<DeviceSyncEventEntity> findByUserCodeOrderByMeasuredAtDesc(String userCode);

	List<DeviceSyncEventEntity> findByDeviceCodeOrderByMeasuredAtDesc(String deviceCode);

	Optional<DeviceSyncEventEntity> findByDeviceCodeAndExternalEventId(String deviceCode, String externalEventId);
}

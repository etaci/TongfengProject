package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.DeviceBindingEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceBindingRepository extends JpaRepository<DeviceBindingEntity, Long> {

	List<DeviceBindingEntity> findByUserCodeOrderByCreatedAtDesc(String userCode);

	Optional<DeviceBindingEntity> findByDeviceCode(String deviceCode);

	Optional<DeviceBindingEntity> findByUserCodeAndSerialNumberAndStatus(String userCode, String serialNumber, String status);
}

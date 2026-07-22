package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.DoctorVisitShareEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorVisitShareRepository extends JpaRepository<DoctorVisitShareEntity, Long> {
	Optional<DoctorVisitShareEntity> findByShareCodeAndUserCode(String shareCode, String userCode);
	Optional<DoctorVisitShareEntity> findByTokenHash(String tokenHash);
}

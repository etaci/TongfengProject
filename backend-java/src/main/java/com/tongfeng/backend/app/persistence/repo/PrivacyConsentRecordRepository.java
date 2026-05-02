package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.PrivacyConsentRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyConsentRecordRepository extends JpaRepository<PrivacyConsentRecordEntity, Long> {

	List<PrivacyConsentRecordEntity> findByUserCodeOrderByEffectiveAtDesc(String userCode);

	Optional<PrivacyConsentRecordEntity> findFirstByUserCodeOrderByEffectiveAtDesc(String userCode);
}

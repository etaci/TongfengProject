package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.GrowthRewardClaimEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthRewardClaimRepository extends JpaRepository<GrowthRewardClaimEntity, Long> {

	List<GrowthRewardClaimEntity> findByUserCodeOrderByClaimedAtDesc(String userCode);

	long countByUserCodeAndRewardKey(String userCode, String rewardKey);
}

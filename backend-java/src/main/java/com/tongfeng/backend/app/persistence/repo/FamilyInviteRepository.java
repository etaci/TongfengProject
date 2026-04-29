package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.FamilyInviteEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyInviteRepository extends JpaRepository<FamilyInviteEntity, Long> {

	Optional<FamilyInviteEntity> findByInviteCode(String inviteCode);

	List<FamilyInviteEntity> findByPatientUserCodeOrderByCreatedAtDesc(String patientUserCode);
}

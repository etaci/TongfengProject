package com.tongfeng.backend.app.persistence.repo;

import com.tongfeng.backend.app.persistence.entity.StoredFileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFileEntity, Long> {

	Optional<StoredFileEntity> findByFileCode(String fileCode);
}

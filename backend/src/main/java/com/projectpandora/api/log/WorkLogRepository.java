package com.projectpandora.api.log;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkLogRepository extends JpaRepository<WorkLogEntity, Long> {
    List<WorkLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WorkLogEntity> findByUserIdAndLogDateOrderByCreatedAtDesc(Long userId, LocalDate logDate);
}

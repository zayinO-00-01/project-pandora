package com.projectpandora.api.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskEventRepository extends JpaRepository<TaskEventEntity,Long> {
    List<TaskEventEntity> findByTaskIdOrderByIdAsc(Long taskId);
}

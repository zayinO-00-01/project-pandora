package com.projectpandora.api.task;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity,Long> {
    @EntityGraph(attributePaths={"creator","assignee"})
    List<TaskEntity> findByAssigneeIdOrderByCreatedAtDescIdDesc(Long assigneeId,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    List<TaskEntity> findByAssigneeIdInOrderByCreatedAtDescIdDesc(List<Long> assigneeIds,Pageable pageable);
}

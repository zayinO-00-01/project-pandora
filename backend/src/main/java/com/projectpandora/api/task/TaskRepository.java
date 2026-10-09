package com.projectpandora.api.task;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    @EntityGraph(attributePaths = {"creator", "assignee"})
    List<TaskEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "assignee"})
    List<TaskEntity> findByAssigneeIdOrderByCreatedAtDescIdDesc(Long assigneeId, Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "assignee"})
    @Query("select t from TaskEntity t where t.creator.id = :userId or t.assignee.id = :userId "
            + "or t.assignee.managerId = :userId order by t.createdAt desc, t.id desc")
    List<TaskEntity> findLeaderScope(@Param("userId") Long userId, Pageable pageable);
}
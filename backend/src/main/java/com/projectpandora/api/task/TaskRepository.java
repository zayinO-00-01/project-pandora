package com.projectpandora.api.task;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<TaskEntity,Long> {
    @EntityGraph(attributePaths={"creator","assignee"})
    List<TaskEntity> findByAssigneeIdOrderByCreatedAtDescIdDesc(Long assigneeId,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    List<TaskEntity> findByAssigneeIdInOrderByCreatedAtDescIdDesc(List<Long> assigneeIds,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    List<TaskEntity> findByAssigneeIdInAndApprovalStatusOrderByCreatedAtDescIdDesc(List<Long> assigneeIds,String approvalStatus,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and t.approvalStatus = 'pending' and t.creator.managerId = :managerId order by t.createdAt desc, t.id desc")
    List<TaskEntity> findPendingTasksForManager(@Param("managerId") Long managerId);
    /** Tasks assigned within my scope plus any task I created, so dispatching outside my subtree stays visible. */
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and (t.assignee.id in :assigneeIds or t.creator.id = :creatorId or t.creator.managerId = :creatorId) order by t.createdAt desc, t.id desc")
    List<TaskEntity> findByScope(@Param("assigneeIds") List<Long> assigneeIds,@Param("creatorId") Long creatorId,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and t.approvalStatus = 'approved' and (t.assignee.id in :assigneeIds or t.creator.id = :creatorId) order by t.createdAt desc, t.id desc")
    List<TaskEntity> findApprovedByScope(@Param("assigneeIds") List<Long> assigneeIds,@Param("creatorId") Long creatorId,Pageable pageable);
    /** Tasks waiting for review, used to build each reviewer's inbox. */
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and t.reviewStatus in :statuses order by t.updatedAt desc, t.id desc")
    List<TaskEntity> findByReviewStatusIn(@Param("statuses") Collection<String> statuses);
}

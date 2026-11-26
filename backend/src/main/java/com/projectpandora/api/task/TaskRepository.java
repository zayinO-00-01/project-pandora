package com.projectpandora.api.task;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<TaskEntity,Long> {
    String ASSIGNED_TO = "(t.assignee.id = :assigneeId or exists (select a.id from TaskEntity shared join shared.assignees a where shared.id = t.id and a.id = :assigneeId))";
    String ASSIGNED_IN = "(t.assignee.id in :assigneeIds or exists (select a.id from TaskEntity shared join shared.assignees a where shared.id = t.id and a.id in :assigneeIds))";
    String NEWEST = " order by t.createdAt desc, t.id desc";
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where " + ASSIGNED_TO + NEWEST)
    List<TaskEntity> findByAssigneeIdOrderByCreatedAtDescIdDesc(@Param("assigneeId") Long assigneeId,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where " + ASSIGNED_IN + NEWEST)
    List<TaskEntity> findByAssigneeIdInOrderByCreatedAtDescIdDesc(@Param("assigneeIds") List<Long> assigneeIds,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.approvalStatus = :approvalStatus and " + ASSIGNED_IN + NEWEST)
    List<TaskEntity> findByAssigneeIdInAndApprovalStatusOrderByCreatedAtDescIdDesc(@Param("assigneeIds") List<Long> assigneeIds,@Param("approvalStatus") String approvalStatus,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and t.approvalStatus = 'pending' and t.creator.managerId = :managerId order by t.createdAt desc, t.id desc")
    List<TaskEntity> findPendingTasksForManager(@Param("managerId") Long managerId);
    /** Tasks assigned within my scope plus any task I created, so dispatching outside my subtree stays visible. */
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and (" + ASSIGNED_IN + " or t.creator.id = :creatorId or t.creator.managerId = :creatorId)" + NEWEST)
    List<TaskEntity> findByScope(@Param("assigneeIds") List<Long> assigneeIds,@Param("creatorId") Long creatorId,Pageable pageable);
    /** The all-tasks view is limited to tasks the viewer created or is responsible for. */
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and (" + ASSIGNED_TO + " or t.creator.id = :assigneeId)" + NEWEST)
    List<TaskEntity> findOwnedOrCreated(@Param("assigneeId") Long userId,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and t.approvalStatus = 'approved' and (" + ASSIGNED_IN + " or t.creator.id = :creatorId)" + NEWEST)
    List<TaskEntity> findApprovedByScope(@Param("assigneeIds") List<Long> assigneeIds,@Param("creatorId") Long creatorId,Pageable pageable);
    /** Tasks waiting for review, used to build each reviewer's inbox. */
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and t.reviewStatus in :statuses order by t.updatedAt desc, t.id desc")
    List<TaskEntity> findByReviewStatusIn(@Param("statuses") Collection<String> statuses);
}

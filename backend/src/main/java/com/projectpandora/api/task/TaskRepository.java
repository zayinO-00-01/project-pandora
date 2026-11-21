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
    @Query("select t from TaskEntity t where t.assignee.id=:assigneeId or exists (select a.id from TaskEntity shared join shared.assignees a where shared.id=t.id and a.id=:assigneeId) order by t.createdAt desc,t.id desc")
    List<TaskEntity> findByAssigneeIdOrderByCreatedAtDescIdDesc(Long assigneeId,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.assignee.id in :assigneeIds or exists (select a.id from TaskEntity shared join shared.assignees a where shared.id=t.id and a.id in :assigneeIds) order by t.createdAt desc,t.id desc")
    List<TaskEntity> findByAssigneeIdInOrderByCreatedAtDescIdDesc(List<Long> assigneeIds,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.approvalStatus=:approvalStatus and (t.assignee.id in :assigneeIds or exists (select a.id from TaskEntity shared join shared.assignees a where shared.id=t.id and a.id in :assigneeIds)) order by t.createdAt desc,t.id desc")
    List<TaskEntity> findByAssigneeIdInAndApprovalStatusOrderByCreatedAtDescIdDesc(List<Long> assigneeIds,String approvalStatus,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and t.approvalStatus = 'pending' and t.creator.managerId = :managerId order by t.createdAt desc, t.id desc")
    List<TaskEntity> findPendingTasksForManager(@Param("managerId") Long managerId);
    /** Tasks assigned within my scope plus any task I created, so dispatching outside my subtree stays visible. */
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and ((t.assignee.id in :assigneeIds or exists (select a.id from TaskEntity shared join shared.assignees a where shared.id=t.id and a.id in :assigneeIds)) or t.creator.id = :creatorId or t.creator.managerId = :creatorId) order by t.createdAt desc, t.id desc")
    List<TaskEntity> findByScope(@Param("assigneeIds") List<Long> assigneeIds,@Param("creatorId") Long creatorId,Pageable pageable);
    /** The all-tasks view is limited to tasks the viewer created or is responsible for. */
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and ((t.assignee.id = :userId or exists (select a.id from TaskEntity shared join shared.assignees a where shared.id=t.id and a.id=:userId)) or t.creator.id = :userId) order by t.createdAt desc, t.id desc")
    List<TaskEntity> findOwnedOrCreated(@Param("userId") Long userId,Pageable pageable);
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and t.approvalStatus = 'approved' and ((t.assignee.id in :assigneeIds or exists (select a.id from TaskEntity shared join shared.assignees a where shared.id=t.id and a.id in :assigneeIds)) or t.creator.id = :creatorId) order by t.createdAt desc, t.id desc")
    List<TaskEntity> findApprovedByScope(@Param("assigneeIds") List<Long> assigneeIds,@Param("creatorId") Long creatorId,Pageable pageable);
    /** Tasks waiting for review, used to build each reviewer's inbox. */
    @EntityGraph(attributePaths={"creator","assignee"})
    @Query("select t from TaskEntity t where t.deletedAt is null and t.reviewStatus in :statuses order by t.updatedAt desc, t.id desc")
    List<TaskEntity> findByReviewStatusIn(@Param("statuses") Collection<String> statuses);
}

package com.projectpandora.api.task;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.user.*;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class TaskService {
    private final TaskRepository tasks;
    private final UserRepository users;
    private final AccessService access;
    private final OrganizationGuard organization;
    public TaskService(TaskRepository tasks,UserRepository users,AccessService access,OrganizationGuard organization) {
        this.tasks=tasks;this.users=users;this.access=access;this.organization=organization;
    }
    public List<TaskResponse> list(Long userId) {
        if(userId!=null) {
            access.assertCanViewUserLogs(userId);
            return tasks.findByAssigneeIdOrderByCreatedAtDescIdDesc(userId,Pageable.unpaged()).stream().map(TaskResponse::from).toList();
        }
        var ids=access.downwardUsers().stream().map(UserEntity::getId).toList();
        if(ids.isEmpty()) return List.of();
        return tasks.findByAssigneeIdInOrderByCreatedAtDescIdDesc(ids,Pageable.unpaged()).stream().map(TaskResponse::from).toList();
    }
    public List<TaskResponse> newest(int count) {
        var ids=access.downwardUsers().stream().map(UserEntity::getId).toList();
        if(ids.isEmpty()) return List.of();
        return tasks.findByAssigneeIdInAndApprovalStatusOrderByCreatedAtDescIdDesc(ids,"approved",PageRequest.of(0,count))
            .stream().map(TaskResponse::from).toList();
    }
    public List<TaskResponse> pendingApprovals() {
        var me=access.assertBusiness();
        return tasks.findPendingSelfTasksForManager(me.getId()).stream()
            .filter(task->canReview(me.getId(),me.getRole(),task)).map(TaskResponse::from).toList();
    }
    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        organization.lock();var me=access.assertBusiness();
        var assignee=users.findById(request.assigneeId()).orElseThrow(()->new ApiException(404,"责任人不存在"));
        boolean self=me.getId().equals(assignee.getId());
        if(!self) access.assertCanDispatch(assignee);
        else if(me.getRole()!=Role.FOUNDER) {
            var manager=assignee.getManagerId()==null?null:users.findById(assignee.getManagerId()).orElse(null);
            if(manager==null || !validManager(manager.getId(),manager.getRole(),assignee) || !access.isActiveBusiness(manager))
                throw new ApiException(400,"自主任务需要有效直属上级审批，请联系管理员配置直属上级");
        }
        var creator=users.findById(me.getId()).orElseThrow(()->new ApiException(401,"账号不可用，请重新登录"));
        var task=new TaskEntity(request,creator,assignee);
        if(self && me.getRole()!=Role.FOUNDER) task.requireApproval();
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
    @Transactional
    public TaskResponse review(Long id,TaskApprovalRequest request) {
        organization.lock();var me=access.assertBusiness();
        var task=tasks.findById(id).orElseThrow(()->new ApiException(404,"任务不存在"));
        if(!canReview(me.getId(),me.getRole(),task)) throw new ApiException(403,"仅当前直属上级可以审批自主任务");
        if(!"pending".equals(task.getApprovalStatus())) throw new ApiException(409,"任务已审批，不能重复操作");
        task.review(request.decision(),request.note(),me.getId());
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
    private boolean canReview(Long reviewerId,Role reviewerRole,TaskEntity task) {
        return task.getCreator().getId().equals(task.getAssignee().getId())
            && validManager(reviewerId,reviewerRole,task.getAssignee());
    }
    private boolean validManager(Long reviewerId,Role reviewerRole,UserEntity author) {
        return access.isActiveBusiness(author) && reviewerRole!=Role.ADMIN && !reviewerId.equals(author.getId())
            && reviewerId.equals(author.getManagerId()) && AccessService.rank(reviewerRole)>AccessService.rank(author.getRole());
    }
    @Transactional
    public TaskResponse updateProgress(Long id,ProgressRequest request) {
        organization.lock();var me=access.assertBusiness();
        var task=tasks.findById(id).orElseThrow(()->new ApiException(404,"任务不存在"));
        if(!task.getAssignee().getId().equals(me.getId())) throw new ApiException(403,"只有责任人可以更新进度");
        if(!"approved".equals(task.getApprovalStatus())) throw new ApiException(409,"任务审批通过后才能更新进度");
        task.updateProgress(request.resolvedStatus(),request.progress(),request.progressNote());
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
}

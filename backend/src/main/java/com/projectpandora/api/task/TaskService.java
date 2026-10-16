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
        return authorizedTasks(Pageable.unpaged());
    }
    public List<TaskResponse> newest(int count) {return authorizedTasks(PageRequest.of(0,count));}
    private List<TaskResponse> authorizedTasks(Pageable pageable) {
        var ids=access.downwardUsers().stream().map(UserEntity::getId).toList();
        if(ids.isEmpty()) return List.of();
        return tasks.findByAssigneeIdInOrderByCreatedAtDescIdDesc(ids,pageable).stream().map(TaskResponse::from).toList();
    }
    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        organization.lock();var me=access.assertBusiness();
        if(!AccessService.isLeader(me.getRole())) throw new ApiException(403,"员工不能派发任务");
        var assignee=users.findById(request.assigneeId()).orElseThrow(()->new ApiException(404,"责任人不存在"));
        access.assertCanDispatch(assignee);
        var creator=users.findById(me.getId()).orElseThrow(()->new ApiException(401,"账号不可用，请重新登录"));
        return TaskResponse.from(tasks.saveAndFlush(new TaskEntity(request,creator,assignee)));
    }
    @Transactional
    public TaskResponse updateProgress(Long id,ProgressRequest request) {
        var me=access.assertBusiness();var task=tasks.findById(id).orElseThrow(()->new ApiException(404,"任务不存在"));
        if(!task.getAssignee().getId().equals(me.getId())) throw new ApiException(403,"只有责任人可以更新进度");
        task.updateProgress(request.resolvedStatus(),request.progress(),request.progressNote());return TaskResponse.from(tasks.saveAndFlush(task));
    }
}

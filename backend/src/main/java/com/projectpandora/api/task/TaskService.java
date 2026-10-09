package com.projectpandora.api.task;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.user.Role;
import com.projectpandora.api.user.UserEntity;
import com.projectpandora.api.user.UserRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository tasks;
    private final UserRepository users;
    private final AccessService access;

    public TaskService(TaskRepository tasks, UserRepository users, AccessService access) {
        this.tasks = tasks; this.users = users; this.access = access;
    }

    public List<TaskResponse> list() { return authorizedTasks(Pageable.unpaged()); }
    public List<TaskResponse> newest(int count) { return authorizedTasks(PageRequest.of(0, count)); }

    private List<TaskResponse> authorizedTasks(Pageable pageable) {
        var me = access.currentUser();
        var result = switch (me.getRole()) {
            case ADMIN -> tasks.findAllByOrderByCreatedAtDescIdDesc(pageable);
            case LEADER -> tasks.findLeaderScope(me.getId(), pageable);
            case STAFF -> tasks.findByAssigneeIdOrderByCreatedAtDescIdDesc(me.getId(), pageable);
        };
        return result.stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        var me = access.currentUser();
        if (me.getRole() == Role.STAFF) throw new ApiException(403, "员工不能派发任务");
        UserEntity assignee = users.findById(request.assigneeId()).orElseThrow(() -> new ApiException(404, "责任人不存在"));
        boolean allowed = me.getRole() == Role.ADMIN
                ? assignee.getRole() != Role.ADMIN && !me.getId().equals(assignee.getId())
                : assignee.getRole() == Role.STAFF && me.getId().equals(assignee.getManagerId());
        if (!allowed) throw new ApiException(403, "无权向该成员派发任务");
        UserEntity creator = users.findById(me.getId()).orElseThrow(() -> new ApiException(401, "用户不存在，请重新登录"));
        return TaskResponse.from(tasks.saveAndFlush(new TaskEntity(request, creator, assignee)));
    }

    @Transactional
    public TaskResponse updateProgress(Long id, ProgressRequest request) {
        var me = access.currentUser();
        var task = tasks.findById(id).orElseThrow(() -> new ApiException(404, "任务不存在"));
        if (!task.getAssignee().getId().equals(me.getId())) throw new ApiException(403, "只有责任人可以更新进度");
        task.updateProgress(request.progress(), request.progressNote());
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
}
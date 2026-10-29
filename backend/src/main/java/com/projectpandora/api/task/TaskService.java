package com.projectpandora.api.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.notification.NotificationService;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.user.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class TaskService {
    private static final List<String> PENDING = List.of("pending_done","pending_change","pending_delete");
    private final TaskRepository tasks;
    private final UserRepository users;
    private final AccessService access;
    private final OrganizationGuard organization;
    private final TaskEventRepository events;
    private final NotificationService notifications;
    private final ObjectMapper json;
    public TaskService(TaskRepository tasks,UserRepository users,AccessService access,OrganizationGuard organization,
            TaskEventRepository events,NotificationService notifications,ObjectMapper json) {
        this.tasks=tasks;this.users=users;this.access=access;this.organization=organization;this.events=events;
        this.notifications=notifications;this.json=json;
    }
    public List<TaskResponse> list(Long userId) {
        if(userId!=null) {
            access.assertCanViewUserLogs(userId);
            return tasks.findByAssigneeIdOrderByCreatedAtDescIdDesc(userId,Pageable.unpaged()).stream()
                .filter(t->!t.isDeleted()).map(TaskResponse::from).toList();
        }
        return authorizedTasks(Pageable.unpaged());
    }
    public List<TaskResponse> newest(int count) {
        var me=access.assertBusiness();
        var ids=access.downwardUsers().stream().map(UserEntity::getId).toList();
        if(ids.isEmpty()) return List.of();
        return tasks.findApprovedByScope(ids,me.getId(),PageRequest.of(0,count)).stream().map(TaskResponse::from).toList();
    }
    private List<TaskResponse> authorizedTasks(Pageable pageable) {
        var me=access.assertBusiness();
        var ids=access.downwardUsers().stream().map(UserEntity::getId).toList();
        if(ids.isEmpty()) return List.of();
        return tasks.findByScope(ids, me.getId(), pageable).stream().map(TaskResponse::from).toList();
    }
    public List<TaskResponse> pendingApprovals() {
        var me=access.assertBusiness();
        return tasks.findPendingSelfTasksForManager(me.getId()).stream()
            .filter(task->canReview(me.getId(),me.getRole(),task)).map(TaskResponse::from).toList();
    }
    /** Tasks whose pending review belongs to the current user. */
    public List<TaskResponse> reviews() {
        var me=access.assertBusiness();
        return tasks.findByReviewStatusIn(PENDING).stream()
            .filter(t->{Long r=reviewerFor(t);return r!=null && r.equals(me.getId());})
            .map(TaskResponse::from).toList();
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
        tasks.saveAndFlush(task);
        logEvent(task.getId(),"created",me.getId(),self?"自主创建任务":"派发任务");
        if(self && me.getRole()!=Role.FOUNDER) notifyUser(assignee.getManagerId(),me.getId(),"task_approval_pending","自主任务待你审批",
            displayName(creator)+" 提交自主任务「"+task.getTitle()+"」，请审批。",task.getId());
        notifyUser(assignee.getId(),me.getId(),"task_assigned","收到新任务",
            "「"+task.getTitle()+"」由 "+displayName(creator)+" 派发给你，请在任务工作台查看。",task.getId());
        return TaskResponse.from(task);
    }
    @Transactional
    public TaskResponse review(Long id,TaskApprovalRequest request) {
        organization.lock();var me=access.assertBusiness();
        var task=activeTask(id);
        if(!canReview(me.getId(),me.getRole(),task)) throw new ApiException(403,"仅当前直属上级可以审批自主任务");
        if(!"pending".equals(task.getApprovalStatus())) throw new ApiException(409,"任务已审批，不能重复操作");
        task.review(request.decision(),request.note(),me.getId());
        logEvent(id,"approval_"+request.decision(),me.getId(),request.note());
        notifyUser(task.getAssignee().getId(),me.getId(),"task_approval_"+request.decision(),"自主任务审批结果",
            "「"+task.getTitle()+"」"+("approved".equals(request.decision())?"已通过审批":"未通过审批")+"。",id);
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
        var task=activeTask(id);
        if(!task.getAssignee().getId().equals(me.getId())) throw new ApiException(403,"只有责任人可以更新进度");
        if(!"approved".equals(task.getApprovalStatus())) throw new ApiException(409,"任务审批通过后才能更新进度");
        String rs=task.getReviewStatus();
        if("pending_delete".equals(rs)) throw new ApiException(409,"任务删除审核中，暂不能更新进度");
        if("approved".equals(rs)) throw new ApiException(403,"任务已完成验收，如需更改请联系派发人重新开启");
        String state=request.resolvedStatus();
        if("pending_change".equals(rs) && "done".equals(state)) throw new ApiException(409,"任务变更审核中，完成验收请稍后");
        if("done".equals(state)) {
            task.updateProgress("done",request.progress(),request.progressNote());
            Long reviewer=completionReviewer(task);
            boolean auto=task.getCreator().getRole()==Role.FOUNDER;
            if(!auto && reviewer==null) throw new ApiException(400,"自主任务需要有效直属上级验收，请联系管理员配置直属上级");
            if(auto) {
                task.setReviewStatus("approved");task.setReviewReason(null);
                task.setReviewedBy(me.getId());task.setLifecycleReviewedAt(Instant.now());
                logEvent(id,"approved",me.getId(),"创始人任务自动验收通过");
            } else {
                task.setReviewStatus("pending_done");task.setReviewReason(null);
                logEvent(id,"review_requested",me.getId(),"提交完成待派发人验收");
                notifyUser(reviewer,me.getId(),"done_pending","待你验收",
                    "「"+task.getTitle()+"」已由 "+displayName(task.getAssignee())+" 提交完成，请验收。",task.getId());
            }
        } else {
            task.updateProgress(state,request.progress(),request.progressNote());
            if("rejected".equals(rs) || "pending_done".equals(rs)) task.setReviewStatus("none");
        }
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
    @Transactional
    public TaskResponse requestChange(Long id,CreateTaskRequest request) {
        organization.lock();var me=access.assertBusiness();
        var task=activeTask(id);
        if(!task.getCreator().getId().equals(me.getId())) throw new ApiException(403,"只有派发人可以修改任务");
        if(!task.getAssignee().getId().equals(request.assigneeId())) throw new ApiException(400,"变更不能修改责任人");
        if(PENDING.contains(task.getReviewStatus())) throw new ApiException(409,"任务已有待审事项，请先处理");
        if(me.getRole()==Role.FOUNDER) {
            task.applyDefinition(request.title(),request.detail(),request.priority(),request.dueAt());
            task.setPendingChange(null);task.setReviewStatus("none");task.setReviewReason(null);
            task.setReviewedBy(me.getId());task.setLifecycleReviewedAt(Instant.now());
            logEvent(id,"approved",me.getId(),"创始人直接修改任务");
        } else {
            task.setPendingChange(payloadJson(request));
            task.setReviewStatus("pending_change");task.setReviewReason(null);
            logEvent(id,"review_requested",me.getId(),"提交任务变更待上级审核");
            notifyUser(superiorOf(task.getCreator()),me.getId(),"change_pending","变更待你审核",
                displayName(task.getCreator())+" 申请修改「"+task.getTitle()+"」，请审核。",task.getId());
        }
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
    @Transactional
    public TaskResponse approve(Long id,ReviewRequest request) {
        organization.lock();var me=access.assertBusiness();
        var task=activeTask(id);
        String rs=task.getReviewStatus();
        if(!rs.startsWith("pending_")) throw new ApiException(400,"当前没有待审事项");
        assertReviewer(task,me.getId());
        String note=request==null?null:request.note();
        switch(rs) {
            case "pending_done" -> task.setReviewStatus("approved");
            case "pending_change" -> {applyPendingChange(task);task.setReviewStatus("none");}
            case "pending_delete" -> {task.softDelete(me.getId());task.setReviewStatus("approved");}
            default -> {}
        }
        task.setReviewReason(note);task.setReviewedBy(me.getId());task.setLifecycleReviewedAt(Instant.now());
        logEvent(id,"approved",me.getId(),note==null||note.isBlank()?"审核通过":note);
        switch(rs) {
            case "pending_done" -> notifyUser(task.getAssignee().getId(),me.getId(),"done_approved","验收通过",
                "你的任务「"+task.getTitle()+"」已通过验收。",task.getId());
            case "pending_change" -> notifyUser(task.getCreator().getId(),me.getId(),"change_approved","变更已通过",
                "「"+task.getTitle()+"」的修改已通过。",task.getId());
            case "pending_delete" -> notifyUser(task.getCreator().getId(),me.getId(),"delete_approved","删除已通过",
                "「"+task.getTitle()+"」的删除申请已通过。",task.getId());
            default -> {}
        }
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
    @Transactional
    public TaskResponse reject(Long id,ReviewRequest request) {
        organization.lock();var me=access.assertBusiness();
        if(request==null||request.note()==null||request.note().isBlank()) throw new ApiException(400,"请填写驳回理由");
        var task=activeTask(id);
        String rs=task.getReviewStatus();
        if(!rs.startsWith("pending_")) throw new ApiException(400,"当前没有待审事项");
        assertReviewer(task,me.getId());
        switch(rs) {
            case "pending_done" -> {task.updateProgress("doing",null,request.note());task.setReviewStatus("rejected");}
            case "pending_change" -> {task.setPendingChange(null);task.setReviewStatus("none");}
            case "pending_delete" -> task.setReviewStatus("none");
            default -> {}
        }
        task.setReviewReason(request.note());task.setReviewedBy(me.getId());task.setLifecycleReviewedAt(Instant.now());
        logEvent(id,"rejected",me.getId(),request.note());
        switch(rs) {
            case "pending_done" -> notifyUser(task.getAssignee().getId(),me.getId(),"done_rejected","验收被驳回",
                "你的任务「"+task.getTitle()+"」被驳回："+request.note(),task.getId());
            case "pending_change" -> notifyUser(task.getCreator().getId(),me.getId(),"change_rejected","变更被驳回",
                "「"+task.getTitle()+"」的修改被驳回："+request.note(),task.getId());
            case "pending_delete" -> notifyUser(task.getCreator().getId(),me.getId(),"delete_rejected","删除被驳回",
                "「"+task.getTitle()+"」的删除申请被驳回："+request.note(),task.getId());
            default -> {}
        }
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
    @Transactional
    public void delete(Long id) {
        organization.lock();var me=access.assertBusiness();
        var task=activeTask(id);
        if(me.getRole()==Role.FOUNDER) {
            task.softDelete(me.getId());task.setReviewStatus("approved");
            logEvent(id,"deleted",me.getId(),"创始人删除任务");
            notifyUser(task.getAssignee().getId(),me.getId(),"task_deleted","任务已删除",
                "任务「"+task.getTitle()+"」已被创始人删除。",task.getId());
            tasks.saveAndFlush(task);return;
        }
        if(!task.getCreator().getId().equals(me.getId())) throw new ApiException(403,"只有派发人或创始人可以删除任务");
        if(PENDING.contains(task.getReviewStatus())) throw new ApiException(409,"任务审核中，暂不能删除");
        boolean untouched="none".equals(task.getReviewStatus()) && "todo".equals(task.getStatus())
            && (task.getProgress()==null || task.getProgress()==0)
            && (task.getProgressNote()==null || task.getProgressNote().isBlank());
        if(untouched) {
            task.softDelete(me.getId());
            logEvent(id,"deleted",me.getId(),"删除未开始的任务");
            notifyUser(task.getAssignee().getId(),me.getId(),"task_deleted","任务已删除",
                "任务「"+task.getTitle()+"」已被派发人删除。",task.getId());
            tasks.saveAndFlush(task);return;
        }
        Long reviewer=superiorOf(task.getCreator());
        if(reviewer==null) {
            task.softDelete(me.getId());task.setReviewStatus("approved");
            logEvent(id,"deleted",me.getId(),"无上级可审，自动删除");
            notifyUser(task.getAssignee().getId(),me.getId(),"task_deleted","任务已删除",
                "任务「"+task.getTitle()+"」已被删除。",task.getId());
            tasks.saveAndFlush(task);return;
        }
        task.setReviewStatus("pending_delete");task.setReviewReason(null);
        logEvent(id,"review_requested",me.getId(),"提交删除待上级审核");
        notifyUser(reviewer,me.getId(),"delete_pending","删除待你审核",
            displayName(task.getCreator())+" 申请删除「"+task.getTitle()+"」，请审核。",task.getId());
        tasks.saveAndFlush(task);
    }
    private TaskEntity activeTask(Long id) {
        var task=tasks.findById(id).orElseThrow(()->new ApiException(404,"任务不存在"));
        if(task.isDeleted()) throw new ApiException(404,"任务不存在");
        return task;
    }
    private Long completionReviewer(TaskEntity task) {
        var creator=task.getCreator();
        if(!creator.getId().equals(task.getAssignee().getId())) return creator.getId();
        var manager=creator.getManagerId()==null?null:users.findById(creator.getManagerId()).orElse(null);
        return manager!=null && access.isActiveBusiness(manager) && validManager(manager.getId(),manager.getRole(),creator)
            ? manager.getId() : null;
    }
    private Long reviewerFor(TaskEntity task) {
        return switch(task.getReviewStatus()) {
            case "pending_done" -> completionReviewer(task);
            case "pending_change","pending_delete" -> superiorOf(task.getCreator());
            default -> null;
        };
    }
    private void assertReviewer(TaskEntity task,Long meId) {
        Long reviewer=reviewerFor(task);
        if(reviewer==null) throw new ApiException(403,"当前没有有效审核人，请联系管理员配置直属上级");
        if(!reviewer.equals(meId) && access.currentUser().getRole()!=Role.FOUNDER)
            throw new ApiException(403,"无权审核该任务");
    }
    private Long superiorOf(UserEntity creator) {
        Long mid=creator.getManagerId();
        Set<Long> seen=new HashSet<>();
        while(mid!=null && seen.add(mid)) {
            var m=users.findById(mid).orElse(null);
            if(m==null) break;
            if(access.isActiveBusiness(m)) return m.getId();
            mid=m.getManagerId();
        }
        return users.findAll().stream()
            .filter(access::isActiveBusiness)
            .filter(u->u.getRole()==Role.FOUNDER)
            .filter(u->!u.getId().equals(creator.getId()))
            .map(UserEntity::getId).findFirst().orElse(null);
    }
    private void applyPendingChange(TaskEntity task) {
        String pending=task.getPendingChange();
        if(pending==null) return;
        try {
            var p=json.readValue(pending,TaskChangePayload.class);
            task.applyDefinition(p.title(),p.detail(),p.priority(),p.dueAt());
        } catch(Exception e) { throw new ApiException(500,"待审变更数据损坏"); }
        task.setPendingChange(null);
    }
    private String payloadJson(CreateTaskRequest r) {
        try { return json.writeValueAsString(new TaskChangePayload(r.title(),r.detail(),r.priority(),r.dueAt())); }
        catch(Exception e) { throw new ApiException(500,"无法保存变更"); }
    }
    private void logEvent(Long taskId,String type,Long actorId,String note) {
        events.save(new TaskEventEntity(taskId,type,actorId,note));
    }
    private void notifyUser(Long recipient,Long actor,String type,String title,String body,Long taskId) {
        if(recipient==null || recipient.equals(actor)) return;
        notifications.notifyUser(recipient,type,title,body,taskId);
    }
    private static String displayName(UserEntity u) {
        return u.getDisplayName()==null||u.getDisplayName().isBlank()?u.getUsername():u.getDisplayName();
    }
}

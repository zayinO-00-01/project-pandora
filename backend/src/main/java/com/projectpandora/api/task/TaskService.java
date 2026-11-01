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
    private final com.projectpandora.api.label.LabelService labels;
    private final TaskKeywordRepository keywords;
    private final TaskReviewRepository keywordReviews;
    private final TaskNoteRepository notes;
    public TaskService(TaskRepository tasks,UserRepository users,AccessService access,OrganizationGuard organization,
            TaskEventRepository events,NotificationService notifications,ObjectMapper json,
            com.projectpandora.api.label.LabelService labels,TaskKeywordRepository keywords,TaskReviewRepository keywordReviews,TaskNoteRepository notes) {
        this.tasks=tasks;this.users=users;this.access=access;this.organization=organization;this.events=events;
        this.notifications=notifications;this.json=json;this.labels=labels;this.keywords=keywords;this.keywordReviews=keywordReviews;
        this.notes=notes;
    }
    public TaskResponse detail(Long id) {
        var me=access.assertBusiness();var task=activeTask(id);
        if(normallyVisible(task) || (PENDING.contains(task.getReviewStatus()) && me.getId().equals(reviewerFor(task)))
                || keywordReviews.findByTaskIdAndStatus(id,"pending").stream()
                .anyMatch(review->me.getId().equals(currentReviewerId(review)))) return TaskResponse.from(task);
        throw new ApiException(403,"无权查看该任务");
    }
    private boolean normallyVisible(TaskEntity task) {
        if(authorizedTasks().stream().anyMatch(item->item.id().equals(task.getId()))) return true;
        // Member-task views also include an approved request to view one's current direct manager.
        var me=access.assertBusiness();
        if(!initialVisible(task,me.getId(),me.getRole())) return false;
        try {access.assertCanViewUserLogs(task.getAssignee().getId());return true;}
        catch(ApiException e) {if(e.getStatus()==403) return false;throw e;}
    }
    private void assertNoteAccess(Long id) {
        access.assertBusiness();var task=activeTask(id);
        if(!"approved".equals(task.getApprovalStatus())) throw new ApiException(409,"任务审批通过后才能使用备注");
        if(!normallyVisible(task)) throw new ApiException(403,"无权查看该任务备注");
    }
    public TaskNoteResponse note(Long id) {
        var me=access.assertBusiness();assertNoteAccess(id);
        return notes.findByTaskIdAndUserId(id,me.getId()).map(TaskNote::response).orElseGet(()->new TaskNoteResponse(id,"",null));
    }
    @Transactional
    public TaskNoteResponse saveNote(Long id,String content) {
        // Serialize first creation and permission checks with organization changes and task deletion.
        organization.lock();var me=access.assertBusiness();assertNoteAccess(id);
        if(content==null || content.length()>2000) throw new ApiException(400,"备注不能为空值且最多2000字");
        var note=notes.findByTaskIdAndUserId(id,me.getId()).orElseGet(()->new TaskNote(id,me.getId()));
        note.update(content.isBlank()?"":content);
        return notes.saveAndFlush(note).response();
    }
    public List<TaskResponse> list(Long userId) {
        var me=access.assertBusiness();
        if(userId!=null) {
            access.assertCanViewUserLogs(userId);
            return tasks.findByAssigneeIdOrderByCreatedAtDescIdDesc(userId,Pageable.unpaged()).stream()
                .filter(t->!t.isDeleted() && initialVisible(t,me.getId(),me.getRole())).map(TaskResponse::from).toList();
        }
        return authorizedTasks();
    }
    public List<TaskResponse> newest(int count) {
        return authorizedTasks().stream().filter(t->"approved".equals(t.approvalStatus())).limit(count).toList();
    }
    private boolean initialVisible(TaskEntity task,Long viewerId,Role role) {
        return "approved".equals(task.getApprovalStatus()) || task.getCreator().getId().equals(viewerId) || canReview(viewerId,role,task);
    }
    private List<TaskResponse> authorizedTasks() {
        var me=access.assertBusiness();
        var ids=access.downwardUsers().stream().map(UserEntity::getId).toList();
        if(ids.isEmpty()) return List.of();
        return tasks.findByScope(ids,me.getId(),Pageable.unpaged()).stream()
            .filter(t->initialVisible(t,me.getId(),me.getRole()))
            .filter(t->!"approved".equals(t.getApprovalStatus()) || ids.contains(t.getAssignee().getId()) || t.getCreator().getId().equals(me.getId()))
            .map(TaskResponse::from).toList();
    }
    public List<TaskResponse> pendingApprovals() {
        var me=access.assertBusiness();
        return tasks.findPendingTasksForManager(me.getId()).stream()
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
        access.assertCanDispatch(assignee);
        var creator=users.findById(me.getId()).orElseThrow(()->new ApiException(401,"账号不可用，请重新登录"));
        if(me.getRole()!=Role.FOUNDER && managerFor(creator)==null)
            throw new ApiException(400,"创建任务需要有效直属上级审批，请联系管理员配置直属上级");
        var task=new TaskEntity(request,creator,assignee);
        task.setLabel(labels.resolve(request.labelId(),null));
        if(me.getRole()!=Role.FOUNDER) task.requireApproval();
        tasks.saveAndFlush(task);
        logEvent(task.getId(),"created",me.getId(),self?"自主创建任务":"创建派发任务");
        if("pending".equals(task.getApprovalStatus())) notifyUser(creator.getManagerId(),me.getId(),"task_approval_pending","任务创建待你审批",
            displayName(creator)+" 提交任务「"+task.getTitle()+"」，请审批。",task.getId());
        else notifyAssignment(task,me.getId());
        return TaskResponse.from(task);
    }
    private void notifyAssignment(TaskEntity task,Long actor) {
        notifyUser(task.getAssignee().getId(),actor,"task_assigned","收到新任务","「"+task.getTitle()+"」已分配给你，请查看。",task.getId());
    }
    @Transactional
    public TaskResponse review(Long id,TaskApprovalRequest request) {
        organization.lock();var me=access.assertBusiness();var task=activeTask(id);
        if(!canReview(me.getId(),me.getRole(),task)) throw new ApiException(403,"仅创建人的当前直属上级可以审批任务");
        if(!"pending".equals(task.getApprovalStatus())) throw new ApiException(409,"任务已审批，不能重复操作");
        task.review(request.decision(),request.note(),me.getId());
        logEvent(id,"approval_"+request.decision(),me.getId(),request.note());
        notifyUser(task.getCreator().getId(),me.getId(),"task_approval_"+request.decision(),"任务创建审批结果",
            "「"+task.getTitle()+"」"+("approved".equals(request.decision())?"已通过审批":"未通过审批")+"。",id);
        if("approved".equals(request.decision())) notifyAssignment(task,me.getId());
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
    private boolean canReview(Long reviewerId,Role reviewerRole,TaskEntity task) {
        return validManager(reviewerId,reviewerRole,task.getCreator());
    }
    private UserEntity managerFor(UserEntity applicant) {
        var manager=applicant.getManagerId()==null?null:users.findById(applicant.getManagerId()).orElse(null);
        return manager!=null && access.isActiveBusiness(manager) && validManager(manager.getId(),manager.getRole(),applicant)?manager:null;
    }
    private boolean validManager(Long reviewerId,Role reviewerRole,UserEntity author) {
        return access.isActiveBusiness(author) && reviewerRole!=Role.ADMIN && !reviewerId.equals(author.getId())
            && reviewerId.equals(author.getManagerId()) && AccessService.rank(reviewerRole)>AccessService.rank(author.getRole());
    }
    @Transactional
    public TaskResponse updateProgress(Long id,ProgressRequest request) {
        organization.lock();var me=access.assertBusiness();var task=activeTask(id);
        if(!task.getAssignee().getId().equals(me.getId())) throw new ApiException(403,"只有责任人可以更新进度");
        if(!"approved".equals(task.getApprovalStatus())) throw new ApiException(409,"任务审批通过后才能更新进度");
        noPending(task);
        String state=request.resolvedStatus();
        if(me.getRole()==Role.FOUNDER) {
            applyProgress(task,new ProgressRequest(request.progress(),request.progressNote(),state),me.getId());
            logEvent(id,"progress_approved",me.getId(),"创始人本人更新进度");
            return TaskResponse.from(tasks.saveAndFlush(task));
        }
        request(task,task.getAssignee(),"PROGRESS",new ProgressRequest(request.progress(),request.progressNote(),state));
        return TaskResponse.from(task);
    }
    private void applyProgress(TaskEntity task,ProgressRequest progress,Long reviewer) {
        task.updateProgress(progress.resolvedStatus(),progress.progress(),progress.progressNote());
        task.setReviewStatus("none");task.setReviewReason(null);task.setReviewedBy(reviewer);task.setLifecycleReviewedAt(Instant.now());
    }
    @Transactional
    public TaskResponse requestChange(Long id,CreateTaskRequest request) {
        organization.lock();var me=access.assertBusiness();var task=activeTask(id);
        boolean creator=task.getCreator().getId().equals(me.getId());
        if(!creator && me.getRole()!=Role.FOUNDER) throw new ApiException(403,"只有创建人或创始人可以修改任务");
        if(!creator && !"approved".equals(task.getApprovalStatus())) throw new ApiException(403,"未生效任务仅申请人可以编辑");
        if(!task.getAssignee().getId().equals(request.assigneeId())) throw new ApiException(400,"变更不能修改责任人");
        if("pending".equals(task.getApprovalStatus())) throw new ApiException(409,"请先撤回创建申请再编辑");
        noPending(task);labels.resolve(request.labelId(),task.getLabel());
        if(!"approved".equals(task.getApprovalStatus())) {
            if(me.getRole()!=Role.FOUNDER && managerFor(task.getCreator())==null) throw new ApiException(400,"创建任务需要有效直属上级审批");
            task.applyDefinition(request.title(),request.detail(),request.priority(),request.dueAt());task.setLabel(labels.resolve(request.labelId(),task.getLabel()));
            if(me.getRole()==Role.FOUNDER) task.review("approved","创始人重新提交",me.getId());
            else {task.requireApproval();notifyUser(task.getCreator().getManagerId(),me.getId(),"task_approval_pending","任务创建待你审批",task.getTitle(),id);}
            logEvent(id,"approval_resubmitted",me.getId(),"编辑后重新提交创建申请");
        } else if(me.getRole()==Role.FOUNDER) {
            task.applyDefinition(request.title(),request.detail(),request.priority(),request.dueAt());task.setLabel(labels.resolve(request.labelId(),task.getLabel()));
            task.setReviewStatus("none");task.setReviewReason(null);task.setReviewedBy(me.getId());task.setLifecycleReviewedAt(Instant.now());
            logEvent(id,"approved",me.getId(),"创始人直接修改任务");
        } else {
            var manager=reviewer(task.getCreator());
            task.setPendingChange(payloadJson(request));task.setReviewStatus("pending_change");task.setReviewReason(null);
            logEvent(id,"review_requested",me.getId(),"提交任务变更待直属上级审核");
            notifyUser(manager.getId(),me.getId(),"change_pending","变更待你审核",displayName(task.getCreator())+" 申请修改「"+task.getTitle()+"」，请审核。",id);
        }
        return TaskResponse.from(tasks.saveAndFlush(task));
    }
    @Transactional
    public TaskResponse withdraw(Long id) {
        organization.lock();var me=access.assertBusiness();var task=activeTask(id);
        if(!task.getCreator().getId().equals(me.getId())) throw new ApiException(403,"仅申请人可以撤回");
        if("pending".equals(task.getApprovalStatus())) task.withdrawApproval();
        else if("pending_change".equals(task.getReviewStatus())) {task.setPendingChange(null);task.setReviewStatus("none");task.setReviewReason("申请已撤回");}
        else throw new ApiException(409,"没有可撤回的创建或内容变更申请");
        logEvent(id,"canceled",me.getId(),"申请人撤回任务申请");
        notifyUser(task.getCreator().getManagerId(),me.getId(),"task_withdrawn","任务申请已撤回",task.getTitle(),id);
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
            case "pending_delete" -> {softDelete(task,me.getId());task.setReviewStatus("approved");}
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
            softDelete(task,me.getId());task.setReviewStatus("approved");
            logEvent(id,"deleted",me.getId(),"创始人删除任务");
            if("approved".equals(task.getApprovalStatus())) notifyUser(task.getAssignee().getId(),me.getId(),"task_deleted","任务已删除",
                "任务「"+task.getTitle()+"」已被创始人删除。",task.getId());
            tasks.saveAndFlush(task);return;
        }
        if(!task.getCreator().getId().equals(me.getId())) throw new ApiException(403,"只有派发人或创始人可以删除任务");
        noPending(task);
        boolean untouched="none".equals(task.getReviewStatus()) && "todo".equals(task.getStatus())
            && (task.getProgress()==null || task.getProgress()==0)
            && (task.getProgressNote()==null || task.getProgressNote().isBlank());
        if(untouched) {
            softDelete(task,me.getId());
            logEvent(id,"deleted",me.getId(),"删除未开始的任务");
            if("approved".equals(task.getApprovalStatus())) notifyUser(task.getAssignee().getId(),me.getId(),"task_deleted","任务已删除",
                "任务「"+task.getTitle()+"」已被派发人删除。",task.getId());
            tasks.saveAndFlush(task);return;
        }
        Long reviewer=superiorOf(task.getCreator());
        if(reviewer==null) {
            softDelete(task,me.getId());task.setReviewStatus("approved");
            logEvent(id,"deleted",me.getId(),"无上级可审，自动删除");
            if("approved".equals(task.getApprovalStatus())) notifyUser(task.getAssignee().getId(),me.getId(),"task_deleted","任务已删除",
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
        var manager=managerFor(task.getAssignee());return manager==null?null:manager.getId();
    }
    private Long reviewerFor(TaskEntity task) {
        return switch(task.getReviewStatus()) {
            case "pending_done" -> completionReviewer(task);
            case "pending_change" -> {var manager=managerFor(task.getCreator());yield manager==null?null:manager.getId();}
            case "pending_delete" -> superiorOf(task.getCreator());
            default -> null;
        };
    }
    private void assertReviewer(TaskEntity task,Long meId) {
        if("pending_done".equals(task.getReviewStatus()) && task.getAssignee().getId().equals(meId) && access.currentUser().getRole()==Role.FOUNDER) return;
        Long reviewer=reviewerFor(task);
        if(reviewer==null) throw new ApiException(403,"当前没有有效审核人，请联系管理员配置直属上级");
        if(!reviewer.equals(meId) && !("pending_delete".equals(task.getReviewStatus()) && access.currentUser().getRole()==Role.FOUNDER))
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
            task.setLabel(labels.resolve(p.labelId(),task.getLabel()));
        } catch(Exception e) { throw new ApiException(500,"待审变更数据损坏"); }
        task.setPendingChange(null);
    }
    private String payloadJson(CreateTaskRequest r) {
        try { return json.writeValueAsString(new TaskChangePayload(r.title(),r.detail(),r.priority(),r.dueAt(),r.labelId())); }
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
    private UserEntity reviewer(UserEntity applicant){
        if(applicant.getManagerId()==null)throw new ApiException(409,"未配置直属上级，无法提交审核；请联系管理员设置审核上级");
        return users.findById(applicant.getManagerId()).filter(access::isActiveBusiness).filter(u->validManager(u.getId(),u.getRole(),applicant)).orElseThrow(()->new ApiException(409,"直属上级不可用，请联系管理员"));
    }
    private TaskEntity task(Long id){return tasks.findById(id).filter(t->!t.isDeleted()).orElseThrow(()->new ApiException(404,"任务不存在"));}
    private void noPending(TaskEntity task){if(PENDING.contains(task.getReviewStatus()) || keywordReviews.existsByTaskIdAndStatus(task.getId(),"pending"))throw new ApiException(409,"任务已有待审核变更，请先完成审核");}
    private void request(TaskEntity task,UserEntity applicant,String operation,Object payload){
        if(!Set.of("KEYWORD_EDIT","KEYWORD_DELETE","PROGRESS").contains(operation)) throw new ApiException(400,"未知关键词操作");
        noPending(task);var manager=reviewer(applicant);var review=new TaskReview();review.taskId=task.getId();review.applicantId=applicant.getId();review.reviewerId=manager.getId();review.operation=operation;review.baseVersion=task.getVersion();
        try{review.payload=json.writeValueAsString(payload);}catch(Exception e){throw new ApiException(400,"变更内容无效");}
        keywordReviews.saveAndFlush(review);logEvent(task.getId(),"review_requested",applicant.getId(),operationLabel(operation));notifications.send(manager.getId(),"任务待审核",applicant.getUsername()+" 提交了"+operationLabel(operation)+"："+task.getTitle(),"task-review",task.getId());
    }
    private String operationLabel(String operation){return switch(operation){case "CREATE"->"创建任务";case "EDIT"->"编辑任务";case "DELETE"->"删除任务";case "KEYWORD_EDIT"->"修改他人关键词";case "KEYWORD_DELETE"->"删除他人关键词";default->"更新进度";};}
    private void participant(TaskEntity task,UserEntity me){
        if(!"approved".equals(task.getApprovalStatus()))throw new ApiException(409,"任务需先获批才能协作制定关键词");
        if(me.getRole()!=Role.FOUNDER && !task.getCreator().getId().equals(me.getId()) && !task.getAssignee().getId().equals(me.getId()))throw new ApiException(403,"仅任务创建人和执行人可协作制定关键词");
    }
    private String keywordText(String input){
        var text=input==null?"":input.strip();
        if(text.isEmpty() || text.length()>24 || text.codePoints().anyMatch(Character::isISOControl))throw new ApiException(400,"关键词需为1至24个字，不能包含换行或控制字符");
        return text;
    }
    private void uniqueKeyword(TaskEntity task,String text,Long except){
        if(task.getKeywords().stream().anyMatch(k->!k.getId().equals(except) && k.getText().equalsIgnoreCase(text)))throw new ApiException(409,"此任务已有相同关键词");
    }
    @Transactional public String addKeyword(Long id,String input){
        organization.lock();var me=users.findById(access.assertBusiness().getId()).orElseThrow();var task=task(id);participant(task,me);noPending(task);
        var text=keywordText(input);uniqueKeyword(task,text,null);
        if(task.getKeywords().size()>=12)throw new ApiException(400,"每个任务最多12个关键词");
        var keyword=keywords.saveAndFlush(new TaskKeyword(id,me.getId(),text));task.getKeywords().add(keyword);task.keywordsChanged();tasks.saveAndFlush(task);
        keywordNotice(task,me,"新增关键词："+text);return "关键词已添加";
    }
    public record KeywordChange(Long keywordId,String keywordBefore,String keywordText,boolean remove) {}
    @Transactional public String changeKeyword(Long id,Long keywordId,String input,boolean remove){
        organization.lock();var me=users.findById(access.assertBusiness().getId()).orElseThrow();var task=task(id);participant(task,me);noPending(task);
        var keyword=task.getKeywords().stream().filter(k->k.getId().equals(keywordId)).findFirst().orElseThrow(()->new ApiException(404,"关键词不存在"));
        var text=remove?"":keywordText(input);if(!remove)uniqueKeyword(task,text,keywordId);
        var change=new KeywordChange(keywordId,keyword.getText(),text,remove);
        if(me.getRole()==Role.FOUNDER || keyword.getCreatedBy().equals(me.getId())){
            applyKeyword(task,change);tasks.saveAndFlush(task);keywordNotice(task,me,(remove?"删除关键词：":"修改关键词：")+keyword.getText());return remove?"关键词已删除":"关键词已更新";
        }
        request(task,users.findById(me.getId()).orElseThrow(),remove?"KEYWORD_DELETE":"KEYWORD_EDIT",change);return "关键词变更已提交直属上级审核，批准前保留原词";
    }
    private void applyKeyword(TaskEntity task,KeywordChange change){
        var keyword=task.getKeywords().stream().filter(k->k.getId().equals(change.keywordId())).findFirst().orElseThrow(()->new ApiException(409,"关键词已变化，请重新提交"));
        if(!keyword.getText().equals(change.keywordBefore()))throw new ApiException(409,"关键词已变化，请重新提交");
        if(change.remove()){task.getKeywords().remove(keyword);keywords.delete(keyword);keywords.flush();}
        else {var text=keywordText(change.keywordText());uniqueKeyword(task,text,keyword.getId());keyword.setText(text);keywords.saveAndFlush(keyword);}
        task.keywordsChanged();
    }
    private void keywordNotice(TaskEntity task,UserEntity me,String message){
        var recipients=new HashSet<Long>(List.of(task.getCreator().getId(),task.getAssignee().getId()));recipients.remove(me.getId());
        recipients.forEach(id->notifications.send(id,"任务关键词已更新",task.getTitle()+" · "+message,"task",task.getId()));
    }
    public record ReviewResponse(Long id,Long taskId,String taskTitle,Long applicantId,String applicantName,Long reviewerId,String reviewerName,String operation,String payload,String status,java.time.Instant createdAt,java.time.Instant decidedAt) {}
    private Long currentReviewerId(TaskReview review) {
        var applicant=users.findById(review.applicantId).orElse(null);var manager=applicant==null?null:managerFor(applicant);
        return manager==null?null:manager.getId();
    }
    private ReviewResponse response(TaskReview review) {
        Long reviewerId="pending".equals(review.status)?currentReviewerId(review):review.reviewerId;
        return new ReviewResponse(review.id,review.taskId,tasks.findById(review.taskId).map(TaskEntity::getTitle).orElse("已删除任务"),review.applicantId,
            users.findById(review.applicantId).map(UserEntity::getUsername).orElse(""),reviewerId,reviewerId==null?"":users.findById(reviewerId).map(UserEntity::getUsername).orElse(""),
            review.operation,readablePayload(review.payload),review.status,review.createdAt,review.decidedAt);
    }
    public List<ReviewResponse> keywordReviews() {
        var me=access.assertBusiness();
        return keywordReviews.findAll().stream()
            .filter(r->r.applicantId.equals(me.getId()) || ("pending".equals(r.status)?me.getId().equals(currentReviewerId(r)):r.reviewerId.equals(me.getId())))
            .sorted(Comparator.comparing((TaskReview r)->r.createdAt).reversed().thenComparing(r->r.id,Comparator.reverseOrder()))
            .map(this::response).toList();
    }
    private String readablePayload(String payload){
        try {var data=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(payload);
            if(data.hasNonNull("assigneeId"))users.findById(data.get("assigneeId").asLong()).ifPresent(u->data.put("assigneeName",u.getDisplayName()==null?u.getUsername():u.getDisplayName()));
            if(data.hasNonNull("labelId")){long id=data.get("labelId").asLong();if(id==0)data.put("labelName","不使用标签");else {var label=labels.resolve(id,null);data.put("labelName",label.getName());data.put("labelColor",label.getColor());}}
            return json.writeValueAsString(data);
        }catch(Exception e){return payload;}
    }
    @Transactional public ReviewResponse decide(Long id,String decision){
        organization.lock();var me=access.assertBusiness();var review=keywordReviews.findById(id).orElseThrow(()->new ApiException(404,"审核不存在"));
        if("canceled".equals(decision)){
            if(!review.applicantId.equals(me.getId()))throw new ApiException(403,"仅申请人可撤回");
            if(!"pending".equals(review.status))throw new ApiException(409,"审核已处理，请刷新");
            var original=task(review.taskId);
            review.status="canceled";review.decidedAt=java.time.Instant.now();keywordReviews.saveAndFlush(review);
            logEvent(review.taskId,"canceled",me.getId(),"撤回"+operationLabel(review.operation));
            Long currentReviewer=currentReviewerId(review);
            if(currentReviewer!=null) notifications.send(currentReviewer,"任务申请已撤回",original.getTitle(),"task-review",original.getId());
            return keywordReviews().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
        }
        if(!Set.of("approved","rejected").contains(decision)) throw new ApiException(400,"无效审核结果");
        if(!me.getId().equals(currentReviewerId(review)) || review.applicantId.equals(me.getId()))throw new ApiException(403,"仅指定直属上级可审核，不能自审");
        var applicant=users.findById(review.applicantId).filter(access::isActiveBusiness).orElseThrow(()->new ApiException(409,"申请人已停用"));
        if(!validManager(me.getId(),me.getRole(),applicant))throw new ApiException(409,"直属上级已变化，不能继续审核");
        if(!"pending".equals(review.status))throw new ApiException(409,"审核已处理，请刷新");
        var task=task(review.taskId);

        if("approved".equals(decision)){
            try{switch(review.operation){
                case "PROGRESS" -> {
                    if(!task.getAssignee().getId().equals(review.applicantId) || !"approved".equals(task.getApprovalStatus())) throw new ApiException(409,"任务责任或审批状态已变化");
                    if(task.getVersion()!=review.baseVersion) throw new ApiException(409,"任务已变化，请撤回后重新提交进度");
                    applyProgress(task,json.readValue(review.payload,ProgressRequest.class),me.getId());
                }
                case "KEYWORD_EDIT", "KEYWORD_DELETE" -> {participant(task,applicant);var input=json.readValue(review.payload,KeywordChange.class);if(input.remove()!=review.operation.equals("KEYWORD_DELETE"))throw new ApiException(400,"关键词操作不匹配");applyKeyword(task,input);}
                default -> throw new ApiException(400,"未知变更类型");
            }}catch(ApiException e){throw e;}catch(Exception e){throw new ApiException(400,"审核内容无效");}
        }
        tasks.saveAndFlush(task);review.reviewerId=me.getId();review.status=decision;review.decidedAt=java.time.Instant.now();keywordReviews.saveAndFlush(review);
        logEvent(task.getId(),review.operation.toLowerCase()+"_"+decision,me.getId(),"审核"+decision);
        String result="approved".equals(decision)?"已批准":"已拒绝";
        notifications.send(review.applicantId,"任务审核"+result,operationLabel(review.operation)+"："+task.getTitle(),"task-review",task.getId());
        if("approved".equals(decision)){
            var recipients=new HashSet<Long>(List.of(task.getAssignee().getId(),task.getCreator().getId()));recipients.remove(review.applicantId);recipients.remove(me.getId());
            recipients.forEach(recipient->notifications.send(recipient,"任务已更新",operationLabel(review.operation)+"："+task.getTitle(),"task",task.getId()));
        }
        return keywordReviews().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
    }
    private void softDelete(TaskEntity task,Long actorId) {
        task.softDelete(actorId);
        keywordReviews.findByTaskIdAndStatus(task.getId(),"pending").forEach(review->{
            review.status="canceled";review.decidedAt=Instant.now();keywordReviews.save(review);
            notifications.send(review.applicantId,"任务已删除，申请已取消",task.getTitle(),"task-review",task.getId());
        });
    }

}

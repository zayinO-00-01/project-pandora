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
    public TaskService(TaskRepository tasks,UserRepository users,AccessService access,OrganizationGuard organization,
            TaskEventRepository events,NotificationService notifications,ObjectMapper json,
            com.projectpandora.api.label.LabelService labels,TaskKeywordRepository keywords,TaskReviewRepository keywordReviews) {
        this.tasks=tasks;this.users=users;this.access=access;this.organization=organization;this.events=events;
        this.notifications=notifications;this.json=json;this.labels=labels;this.keywords=keywords;this.keywordReviews=keywordReviews;
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
        task.setLabel(labels.resolve(request.labelId(),null));
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
        labels.resolve(request.labelId(),task.getLabel());
        if(me.getRole()==Role.FOUNDER) {
            task.applyDefinition(request.title(),request.detail(),request.priority(),request.dueAt());
            task.setLabel(labels.resolve(request.labelId(),task.getLabel()));
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
            softDelete(task,me.getId());
            logEvent(id,"deleted",me.getId(),"删除未开始的任务");
            notifyUser(task.getAssignee().getId(),me.getId(),"task_deleted","任务已删除",
                "任务「"+task.getTitle()+"」已被派发人删除。",task.getId());
            tasks.saveAndFlush(task);return;
        }
        Long reviewer=superiorOf(task.getCreator());
        if(reviewer==null) {
            softDelete(task,me.getId());task.setReviewStatus("approved");
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
    private void noPending(TaskEntity task){if(keywordReviews.existsByTaskIdAndStatus(task.getId(),"pending"))throw new ApiException(409,"任务已有待审核变更，请先完成审核");}
    private void request(TaskEntity task,UserEntity applicant,String operation,Object payload){
        if(!Set.of("KEYWORD_EDIT","KEYWORD_DELETE").contains(operation)) throw new ApiException(400,"未知关键词操作");
        noPending(task);var manager=reviewer(applicant);var review=new TaskReview();review.taskId=task.getId();review.applicantId=applicant.getId();review.reviewerId=manager.getId();review.operation=operation;review.baseVersion=task.getVersion();
        try{review.payload=json.writeValueAsString(payload);}catch(Exception e){throw new ApiException(400,"变更内容无效");}
        keywordReviews.saveAndFlush(review);notifications.send(manager.getId(),"任务待审核",applicant.getUsername()+" 提交了"+operationLabel(operation)+"："+task.getTitle(),"task-review",task.getId());
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
    public List<ReviewResponse> keywordReviews(){var me=access.assertBusiness();return keywordReviews.findByApplicantIdOrReviewerIdOrderByCreatedAtDescIdDesc(me.getId(),me.getId()).stream().map(r->new ReviewResponse(r.id,r.taskId,tasks.findById(r.taskId).map(TaskEntity::getTitle).orElse("已删除任务"),r.applicantId,users.findById(r.applicantId).map(UserEntity::getUsername).orElse(""),r.reviewerId,users.findById(r.reviewerId).map(UserEntity::getUsername).orElse(""),r.operation,readablePayload(r.payload),r.status,r.createdAt,r.decidedAt)).toList();}
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
            notifications.send(review.reviewerId,"任务申请已撤回",original.getTitle(),"task-review",original.getId());
            return keywordReviews().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
        }
        if(!Set.of("approved","rejected").contains(decision)) throw new ApiException(400,"无效审核结果");
        if(!review.reviewerId.equals(me.getId()) || review.applicantId.equals(me.getId()))throw new ApiException(403,"仅指定直属上级可审核，不能自审");
        var applicant=users.findById(review.applicantId).filter(access::isActiveBusiness).orElseThrow(()->new ApiException(409,"申请人已停用"));
        if(!validManager(me.getId(),me.getRole(),applicant))throw new ApiException(409,"直属上级已变化，不能继续审核");
        if(!"pending".equals(review.status))throw new ApiException(409,"审核已处理，请刷新");
        var task=task(review.taskId);
        // Approval validates the keyword ID and original text in applyKeyword;
        // unrelated task progress must not strand approval or rejection.
        if("approved".equals(decision)){
            try{switch(review.operation){
                case "KEYWORD_EDIT", "KEYWORD_DELETE" -> {participant(task,applicant);var input=json.readValue(review.payload,KeywordChange.class);if(input.remove()!=review.operation.equals("KEYWORD_DELETE"))throw new ApiException(400,"关键词操作不匹配");applyKeyword(task,input);}
                default -> throw new ApiException(400,"未知变更类型");
            }}catch(ApiException e){throw e;}catch(Exception e){throw new ApiException(400,"审核内容无效");}
        }
        tasks.saveAndFlush(task);review.status=decision;review.decidedAt=java.time.Instant.now();keywordReviews.saveAndFlush(review);
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

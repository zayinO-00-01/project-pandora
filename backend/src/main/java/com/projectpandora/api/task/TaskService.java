package com.projectpandora.api.task;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.user.*;
import com.projectpandora.api.label.LabelService;
import com.projectpandora.api.notification.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class TaskService {
    private final TaskRepository tasks;private final UserRepository users;private final AccessService access;private final OrganizationGuard organization;
    private final TaskKeywordRepository keywords;private final TaskReviewRepository reviews;private final NotificationService notifications;private final ObjectMapper json;private final LabelService labels;
    public TaskService(TaskRepository tasks,UserRepository users,AccessService access,OrganizationGuard organization,TaskReviewRepository reviews,NotificationService notifications,ObjectMapper json,LabelService labels,TaskKeywordRepository keywords){this.keywords=keywords;this.tasks=tasks;this.users=users;this.access=access;this.organization=organization;this.reviews=reviews;this.notifications=notifications;this.json=json;this.labels=labels;}
    public List<TaskResponse> list(Long userId) {
        if(userId!=null){access.assertCanViewUserLogs(userId);return tasks.findByAssigneeIdOrderByCreatedAtDescIdDesc(userId,Pageable.unpaged()).stream().filter(t->!t.isDeleted() && "approved".equals(t.getApprovalStatus())).map(TaskResponse::from).toList();}
        return authorizedTasks();
    }
    public List<TaskResponse> newest(int count){return authorizedTasks().stream().limit(count).toList();}
    private List<TaskResponse> authorizedTasks(){
        var me=access.assertBusiness();var ids=access.taskViewUserIds();
        var visible=new HashMap<Long,TaskEntity>();
        tasks.findByAssigneeIdInOrCreatorIdOrderByCreatedAtDescIdDesc(ids,me.getId(),Pageable.unpaged()).forEach(t->{if("approved".equals(t.getApprovalStatus()) || t.getCreator().getId().equals(me.getId()))visible.put(t.getId(),t);});
        reviews.findByReviewerIdAndStatus(me.getId(),"pending").forEach(r->tasks.findById(r.taskId).ifPresent(t->visible.put(t.getId(),t)));
        return visible.values().stream().filter(t->!t.isDeleted()).sorted(Comparator.comparing(TaskEntity::getCreatedAt).reversed().thenComparing(TaskEntity::getId,Comparator.reverseOrder())).map(TaskResponse::from).toList();
    }
    private UserEntity assignee(CreateTaskRequest request){var person=users.findById(request.assigneeId()).orElseThrow(()->new ApiException(404,"责任人不存在"));access.assertCanDispatch(person);return person;}
    private UserEntity reviewer(UserEntity applicant){
        if(applicant.getManagerId()==null)throw new ApiException(409,"未配置直属上级，无法提交审核；请联系管理员设置审核上级");
        return users.findById(applicant.getManagerId()).filter(access::isActiveBusiness).filter(u->!u.getId().equals(applicant.getId())).orElseThrow(()->new ApiException(409,"直属上级不可用，请联系管理员"));
    }
    private TaskEntity task(Long id){return tasks.findById(id).filter(t->!t.isDeleted()).orElseThrow(()->new ApiException(404,"任务不存在"));}
    private void noPending(TaskEntity task){if(reviews.existsByTaskIdAndStatus(task.getId(),"pending"))throw new ApiException(409,"任务已有待审核变更，请先完成审核");}
    private void request(TaskEntity task,UserEntity applicant,String operation,Object payload){
        noPending(task);var manager=reviewer(applicant);var review=new TaskReview();review.taskId=task.getId();review.applicantId=applicant.getId();review.reviewerId=manager.getId();review.operation=operation;review.baseVersion=task.getVersion();
        try{review.payload=json.writeValueAsString(payload);}catch(Exception e){throw new ApiException(400,"变更内容无效");}
        reviews.saveAndFlush(review);notifications.send(manager.getId(),"任务待审核",applicant.getUsername()+" 提交了"+operationLabel(operation)+"："+task.getTitle(),"task-review",task.getId());
    }
    private String operationLabel(String operation){return switch(operation){case "CREATE"->"创建任务";case "EDIT"->"编辑任务";case "DELETE"->"删除任务";case "KEYWORD_EDIT"->"修改他人关键词";case "KEYWORD_DELETE"->"删除他人关键词";default->"更新进度";};}
    @Transactional public TaskResponse create(CreateTaskRequest request){
        organization.lock();var principal=access.assertBusiness();var creator=users.findById(principal.getId()).orElseThrow();var task=new TaskEntity(request,creator,assignee(request));task.setLabel(labels.resolve(request.labelId(),null));
        if(creator.getRole()!=Role.FOUNDER){reviewer(creator);task.setApprovalStatus("pending");}
        tasks.saveAndFlush(task);
        if("pending".equals(task.getApprovalStatus()))request(task,creator,"CREATE",request);
        else notifications.send(task.getAssignee().getId(),"收到新任务",task.getTitle(),"task",task.getId());
        return TaskResponse.from(task);
    }
    @Transactional public TaskResponse edit(Long id,CreateTaskRequest request){
        organization.lock();var me=access.assertBusiness();var task=task(id);
        if(!task.getCreator().getId().equals(me.getId()) && me.getRole()!=Role.FOUNDER)throw new ApiException(403,"仅创建人或创始人可编辑任务");
        if("pending".equals(task.getApprovalStatus()))throw new ApiException(409,"任务创建尚在审核中");
        var person=assignee(request);var label=labels.resolve(request.labelId(),task.getLabel());
        if(me.getRole()==Role.FOUNDER){noPending(task);task.edit(request,person,label);task.setApprovalStatus("approved");tasks.saveAndFlush(task);notifications.send(person.getId(),"任务已更新",task.getTitle(),"task",task.getId());}
        else request(task,users.findById(me.getId()).orElseThrow(),"EDIT",request);
        return TaskResponse.from(task);
    }
    @Transactional public TaskResponse updateProgress(Long id,ProgressRequest input){
        organization.lock();var me=access.assertBusiness();var task=task(id);
        if(!task.getAssignee().getId().equals(me.getId()))throw new ApiException(403,"只有责任人可以更新进度");
        if(!"approved".equals(task.getApprovalStatus()))throw new ApiException(409,"任务创建尚未获批，暂不能反馈进度");
        var status=input.resolvedStatus();
        if(me.getRole()==Role.FOUNDER){noPending(task);task.updateProgress(status,input.progress(),input.progressNote());tasks.saveAndFlush(task);notifications.send(task.getCreator().getId(),"任务进度已更新",task.getTitle(),"task",task.getId());}
        else request(task,users.findById(me.getId()).orElseThrow(),"PROGRESS",input);
        return TaskResponse.from(task);
    }
    @Transactional public void delete(Long id){
        organization.lock();var me=access.assertBusiness();if(me.getRole()!=Role.FOUNDER)throw new ApiException(403,"仅创始人可申请删除任务");var task=task(id);
        reviews.findByTaskIdAndStatus(task.getId(),"pending").forEach(r->{r.status="canceled";r.decidedAt=java.time.Instant.now();reviews.save(r);notifications.send(r.applicantId,"任务已删除，申请已取消",task.getTitle(),"task-review",task.getId());});
        task.markDeleted();tasks.saveAndFlush(task);
        var recipients=new HashSet<Long>(List.of(task.getCreator().getId(),task.getAssignee().getId()));recipients.remove(me.getId());
        recipients.forEach(recipient->notifications.send(recipient,"任务已删除",task.getTitle(),"task",task.getId()));
    }
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
    public List<ReviewResponse> reviews(){var me=access.assertBusiness();return reviews.findByApplicantIdOrReviewerIdOrderByCreatedAtDescIdDesc(me.getId(),me.getId()).stream().map(r->new ReviewResponse(r.id,r.taskId,tasks.findById(r.taskId).map(TaskEntity::getTitle).orElse("已删除任务"),r.applicantId,users.findById(r.applicantId).map(UserEntity::getUsername).orElse(""),r.reviewerId,users.findById(r.reviewerId).map(UserEntity::getUsername).orElse(""),r.operation,readablePayload(r.payload),r.status,r.createdAt,r.decidedAt)).toList();}
    private String readablePayload(String payload){
        try {var data=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(payload);
            if(data.hasNonNull("assigneeId"))users.findById(data.get("assigneeId").asLong()).ifPresent(u->data.put("assigneeName",u.getDisplayName()==null?u.getUsername():u.getDisplayName()));
            if(data.hasNonNull("labelId")){long id=data.get("labelId").asLong();if(id==0)data.put("labelName","不使用标签");else {var label=labels.resolve(id,null);data.put("labelName",label.getName());data.put("labelColor",label.getColor());}}
            return json.writeValueAsString(data);
        }catch(Exception e){return payload;}
    }
    @Transactional public ReviewResponse decide(Long id,String decision){
        organization.lock();var me=access.assertBusiness();var review=reviews.findById(id).orElseThrow(()->new ApiException(404,"审核不存在"));
        if("canceled".equals(decision)){
            if(!review.applicantId.equals(me.getId()))throw new ApiException(403,"仅申请人可撤回");
            if(!"pending".equals(review.status))throw new ApiException(409,"审核已处理，请刷新");
            var original=task(review.taskId);if("CREATE".equals(review.operation)){original.setApprovalStatus("rejected");tasks.saveAndFlush(original);}
            review.status="canceled";review.decidedAt=java.time.Instant.now();reviews.saveAndFlush(review);
            notifications.send(review.reviewerId,"任务申请已撤回",original.getTitle(),"task-review",original.getId());
            return reviews().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
        }
        if(!review.reviewerId.equals(me.getId()) || review.applicantId.equals(me.getId()))throw new ApiException(403,"仅指定直属上级可审核，不能自审");
        var applicant=users.findById(review.applicantId).filter(access::isActiveBusiness).orElseThrow(()->new ApiException(409,"申请人已停用"));
        if(!me.getId().equals(applicant.getManagerId()))throw new ApiException(409,"直属上级已变化，不能继续审核");
        if(!"pending".equals(review.status))throw new ApiException(409,"审核已处理，请刷新");
        var task=task(review.taskId);
        if(task.getVersion()!=review.baseVersion)throw new ApiException(409,"任务已变化，请重新核对");
        if("approved".equals(decision)){
            try{switch(review.operation){
                case "CREATE" -> {access.assertCanDispatch(task.getAssignee());task.setApprovalStatus("approved");}
                case "EDIT" -> {var input=json.readValue(review.payload,CreateTaskRequest.class);task.edit(input,assignee(input),labels.resolve(input.labelId(),task.getLabel()));task.setApprovalStatus("approved");}
                case "PROGRESS" -> {if(!task.getAssignee().getId().equals(review.applicantId))throw new ApiException(409,"责任人已变化");var input=json.readValue(review.payload,ProgressRequest.class);task.updateProgress(input.resolvedStatus(),input.progress(),input.progressNote());}
                case "KEYWORD_EDIT", "KEYWORD_DELETE" -> {participant(task,applicant);var input=json.readValue(review.payload,KeywordChange.class);if(input.remove()!=review.operation.equals("KEYWORD_DELETE"))throw new ApiException(400,"关键词操作不匹配");applyKeyword(task,input);}
                case "DELETE" -> {if(applicant.getRole()!=Role.FOUNDER)throw new ApiException(403,"申请人已失去删除权限");task.markDeleted();}
                default -> throw new ApiException(400,"未知变更类型");
            }}catch(ApiException e){throw e;}catch(Exception e){throw new ApiException(400,"审核内容无效");}
        } else if("CREATE".equals(review.operation))task.setApprovalStatus("rejected");
        tasks.saveAndFlush(task);review.status=decision;review.decidedAt=java.time.Instant.now();reviews.saveAndFlush(review);
        String result="approved".equals(decision)?"已批准":"已拒绝";
        notifications.send(review.applicantId,"任务审核"+result,operationLabel(review.operation)+"："+task.getTitle(),"task-review",task.getId());
        if("approved".equals(decision)){
            var recipients=new HashSet<Long>(List.of(task.getAssignee().getId(),task.getCreator().getId()));recipients.remove(review.applicantId);recipients.remove(me.getId());
            recipients.forEach(recipient->notifications.send(recipient,"任务已更新",operationLabel(review.operation)+"："+task.getTitle(),"task",task.getId()));
        }
        return reviews().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
    }
}

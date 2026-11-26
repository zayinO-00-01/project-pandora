package com.projectpandora.api.label;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.task.TaskService;
import com.projectpandora.api.user.OrganizationGuard;
import com.projectpandora.api.user.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/content-labels")
public class ContentLabelsController {
 private final JdbcTemplate db;private final AccessService access;private final TaskService tasks;private final LabelRepository labels;private final ObjectMapper json;private final OrganizationGuard guard;private final ContentLabelStore store;
 public ContentLabelsController(JdbcTemplate db,AccessService access,TaskService tasks,LabelRepository labels,ObjectMapper json,OrganizationGuard guard,ContentLabelStore store){this.db=db;this.access=access;this.tasks=tasks;this.labels=labels;this.json=json;this.guard=guard;this.store=store;}
 public record Entry(String kind,Long contentId,List<LabelResponse> labels,@JsonInclude(JsonInclude.Include.NON_NULL) String status){}
 public record Input(@NotNull @Size(max=12) List<@NotNull Long> labelIds){}
 private record StoredEntry(String kind,Long contentId,String labelIds,Long logOwner,String logStatus,Long personalOwner,boolean companyEnabled){}
 private Entry entry(String kind,Long id,String ids) {
  try {return entry(kind,id,Arrays.asList(json.readValue(ids,Long[].class)));}
  catch(JsonProcessingException e){throw new ApiException(500,"标签记录读取失败");}
 }
 private Entry entry(String kind,Long id,List<Long> ids) {
  return entry(kind,id,ids,null);
 }
 private Entry entry(String kind,Long id,List<Long> ids,String status) {
  return new Entry(kind,id,ids.stream().map(labels::findById).flatMap(Optional::stream).map(LabelResponse::from).toList(),status);
 }
 @GetMapping @Transactional(readOnly=true)
 public List<Entry> list() {
  Long me=access.assertBusiness().getId();
  var rows=db.query("""
      select s.kind,s.content_id,s.label_ids,l.user_id,l.status,p.owner_id,n.enabled
      from content_label_sets s
      left join work_logs l on s.kind='log' and s.content_id=l.id
      left join personal_priorities p on s.kind='personal' and s.content_id=p.id
      left join company_notices n on s.kind='company' and s.content_id=n.id
      order by s.kind,s.content_id
      """,(rs,n)->new StoredEntry(rs.getString(1),rs.getLong(2),rs.getString(3),
          rs.getObject(4,Long.class),rs.getString(5),rs.getObject(6,Long.class),rs.getBoolean(7)));
  var logScope=new HashMap<Long,Boolean>();
  return rows.stream().filter(row->switch(row.kind()) {
   case "task"->tasks.readableDetail(row.contentId()).isPresent();
   case "log"->me.equals(row.logOwner()) || (row.logOwner()!=null && "submitted".equals(row.logStatus())
       && logScope.computeIfAbsent(row.logOwner(),this::canReadUserLogs));
   case "personal"->me.equals(row.personalOwner());
   case "company"->row.companyEnabled();
   default->false;
  }).map(row->entry(row.kind(),row.contentId(),row.labelIds())).toList();
 }
 private boolean canReadUserLogs(Long userId) {
  try {access.assertCanViewUserLogs(userId);return true;}
  catch(ApiException e){if(e.getStatus()==403 || e.getStatus()==404)return false;throw e;}
 }
 @PutMapping("/{kind}/{id}") @Transactional public Entry save(@PathVariable String kind,@PathVariable Long id,@Valid @RequestBody Input input){
  if("task".equals(kind)) {
   var change=tasks.changeLabels(id,input.labelIds());
   return entry(kind,id,change.labelIds(),change.status());
  }
  guard.lock();var me=access.assertBusiness();
  boolean canWrite=switch(kind) {
   case "log"->exists("select id from work_logs where id=? and user_id=?",id,me.getId());
   case "personal"->exists("select id from personal_priorities where id=? and owner_id=?",id,me.getId());
   case "company"->me.getRole()==Role.FOUNDER && exists("select id from company_notices where id=? and enabled=true",id);
   default->false;
  };
  if(!canWrite) throw new ApiException(403,"无权修改此内容标签");
  var ids=input.labelIds().stream().distinct().toList();store.save(kind,id,ids);
  return entry(kind,id,ids);
 }
 private boolean exists(String sql,Object... args){return !db.queryForList(sql,Long.class,args).isEmpty();}
}

package com.projectpandora.api.label;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.task.TaskService;
import com.projectpandora.api.user.OrganizationGuard;
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
 public record Entry(String kind,Long contentId,List<LabelResponse> labels){}
 public record Input(@NotNull @Size(max=12) List<@NotNull Long> labelIds){}
 private Set<Long> readable(String kind){long me=access.assertBusiness().getId();return switch(kind){
 case "task"->new HashSet<>(tasks.list(null).stream().map(t->t.id()).toList());
 case "log"->new HashSet<>(db.queryForList("select id from work_logs where user_id=?",Long.class,me));
 case "personal"->new HashSet<>(db.queryForList("select id from personal_priorities where owner_id=?",Long.class,me));
 case "company"->new HashSet<>(db.queryForList("select id from company_notices where enabled=true",Long.class));
 default->Set.of();};}
 private Entry entry(String kind,Long id,String ids){try {var values=json.readValue(ids,Long[].class);return new Entry(kind,id,Arrays.stream(values).map(v->labels.findById(v).map(LabelResponse::from).orElse(null)).filter(Objects::nonNull).toList());}catch(Exception e){throw new ApiException(500,"标签记录读取失败");}}
 @GetMapping @Transactional(readOnly=true) public List<Entry> list(){access.assertBusiness();Map<String,Set<Long>> scope=new HashMap<>();for(String k:List.of("task","log","personal","company"))scope.put(k,readable(k));return db.query("select kind,content_id,label_ids from content_label_sets",(rs,n)->entry(rs.getString(1),rs.getLong(2),rs.getString(3))).stream().filter(e->scope.getOrDefault(e.kind(),Set.of()).contains(e.contentId())).toList();}
 @PutMapping("/{kind}/{id}") @Transactional public Entry save(@PathVariable String kind,@PathVariable Long id,@Valid @RequestBody Input input)throws Exception{
 guard.lock();var me=access.assertBusiness();if(!readable(kind).contains(id))throw new ApiException(403,"无权修改此内容标签");
 if(kind.equals("company") && me.getRole()!=com.projectpandora.api.user.Role.FOUNDER)throw new ApiException(403,"公司事项标签由创始人维护");
 if(kind.equals("task")){var t=tasks.list(null).stream().filter(v->v.id().equals(id)).findFirst().orElseThrow();if(!t.createdBy().equals(me.getId()) && !t.assigneeId().equals(me.getId()) && me.getRole()!=com.projectpandora.api.user.Role.FOUNDER)throw new ApiException(403,"仅任务创建人、负责人或创始人可设置标签");}
 var ids=input.labelIds().stream().distinct().toList();store.save(kind,id,ids);
 return entry(kind,id,json.writeValueAsString(ids));
 }
}

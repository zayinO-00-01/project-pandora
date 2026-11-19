package com.projectpandora.api.todo;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import com.projectpandora.api.label.LabelResponse;
import com.projectpandora.api.label.LabelRepository;
import com.projectpandora.api.label.WorkLabel;
@RestController @RequestMapping("/api/v1/personal-todos") @Transactional
public class PersonalTodoController {
 private final JdbcTemplate db;private final AccessService access;private final LabelRepository labels;
 private final com.projectpandora.api.user.UserRepository users;
 public PersonalTodoController(JdbcTemplate db,AccessService access,LabelRepository labels,com.projectpandora.api.user.UserRepository users){
    this.db=db;this.access=access;this.labels=labels;this.users=users;
 }
 public record Todo(String id,String title,LocalDate date,boolean completed,List<LabelResponse> labels){}
 public record Snapshot(@NotBlank @Size(max=200) String title,@NotNull LocalDate date,boolean completed,@NotNull @Size(max=12) List<@NotNull Long> labelIds){}
 public record Input(@NotBlank @Size(max=200) String title,@NotNull LocalDate date,boolean completed,
    @Size(max=12) List<@NotNull Long> labelIds,@Valid Snapshot base,
    @Pattern(regexp="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") String requestId){}
 private long lockOwner(){
    long owner=access.assertBusiness().getId();
    users.lockById(owner).orElseThrow(()->new ApiException(401,"账号不可用"));
    return owner;
 }
 private List<LabelResponse> tags(String id){
    return tags(id,false);
 }
 private List<LabelResponse> tags(String id,boolean lock){
    return db.queryForList("select label_id from personal_todo_labels where todo_id=? order by sort_order"+(lock?" for update":""),Long.class,id)
        .stream().map(label->labels.findById(label).orElseThrow()).map(LabelResponse::from).toList();
 }
 private void saveTags(String id,List<Long> ids){
    if(ids==null)return;
    var previous=tags(id,true).stream().map(LabelResponse::id).toList();
    var unique=ids.stream().distinct().toList();
    if(previous.equals(unique))return;
    for(Long label:unique)if(!previous.contains(label) && labels.findById(label).filter(WorkLabel::isActive).isEmpty())
        throw new ApiException(400,"标签不存在或已停用，请重新选择");
    db.update("delete from personal_todo_labels where todo_id=?",id);
    for(int i=0;i<unique.size();i++)db.update("insert into personal_todo_labels(todo_id,label_id,sort_order) values(?,?,?)",id,unique.get(i),i);
 }
 private Todo get(String id,long owner){return db.query("select id,title,todo_date,completed from personal_todos where id=? and owner_id=? for update",(r,n)->new Todo(r.getString(1),r.getString(2),r.getDate(3).toLocalDate(),r.getBoolean(4),tags(r.getString(1),true)),id,owner).stream().findFirst().orElseThrow(()->new ApiException(404,"事项不存在"));}
 @GetMapping public List<Todo> list(){long owner=access.assertBusiness().getId();return db.query("select id,title,todo_date,completed from personal_todos where owner_id=? order by todo_date,id",(r,n)->new Todo(r.getString(1),r.getString(2),r.getDate(3).toLocalDate(),r.getBoolean(4),tags(r.getString(1))),owner);}
 @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
 public Todo create(@Valid @RequestBody Input input){
    long owner=lockOwner();
    if(input.requestId()!=null) {
        var saved=db.queryForList("select todo_id from personal_todo_requests where owner_id=? and request_id=? for update",String.class,owner,input.requestId());
        if(!saved.isEmpty()) {
            try {return get(saved.get(0),owner);}
            catch(ApiException e) {if(e.getStatus()!=404)throw e;throw new ApiException(409,"这条事项已创建过，后来被删除。要重新创建，请关闭后重新添加。");}
        }
    }
    String id=UUID.randomUUID().toString();
    db.update("insert into personal_todos(id,owner_id,title,todo_date,completed) values(?,?,?,?,?)",id,owner,input.title().trim(),input.date(),input.completed());
    saveTags(id,input.labelIds());
    if(input.requestId()!=null)db.update("insert into personal_todo_requests(owner_id,request_id,todo_id) values(?,?,?)",owner,input.requestId(),id);
    return get(id,owner);
 }
 @PutMapping("/{id}") public Todo update(@PathVariable String id,@Valid @RequestBody Input input){
    long owner=lockOwner();
    var latest=get(id,owner);
    String title=input.title().trim();
    LocalDate date=input.date();
    boolean completed=input.completed();
    var chosen=input.labelIds();
    if(input.base()!=null){
        var base=input.base();
        title=merge(base.title().trim(),title,latest.title());
        date=merge(base.date(),date,latest.date());
        completed=merge(base.completed(),completed,latest.completed());
        if(chosen!=null) chosen=merge(base.labelIds(),chosen,latest.labels().stream().map(LabelResponse::id).toList());
    }
    db.update("update personal_todos set title=?,todo_date=?,completed=? where id=? and owner_id=?",title,date,completed,id,owner);
    saveTags(id,chosen);
    return get(id,owner);
 }
 private static <T> T merge(T original,T proposed,T latest){
    if(Objects.equals(original,proposed))return latest;
    if(Objects.equals(original,latest) || Objects.equals(proposed,latest))return proposed;
    throw new ApiException(409,"这项内容已在其他设备修改，请对比后再保存。");
 }
 @DeleteMapping("/{id}") public Todo delete(@PathVariable String id){long owner=lockOwner();var item=get(id,owner);db.update("delete from personal_todos where id=? and owner_id=?",id,owner);return item;}
}

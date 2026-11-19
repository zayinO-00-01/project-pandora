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
 public PersonalTodoController(JdbcTemplate db,AccessService access,LabelRepository labels){this.db=db;this.access=access;this.labels=labels;}
 public record Todo(String id,String title,LocalDate date,boolean completed,List<LabelResponse> labels){}
 public record Input(@NotBlank @Size(max=200) String title,@NotNull LocalDate date,boolean completed,@Size(max=12) List<@NotNull Long> labelIds){}
 private List<LabelResponse> tags(String id){
    return db.queryForList("select label_id from personal_todo_labels where todo_id=? order by sort_order",Long.class,id)
        .stream().map(label->labels.findById(label).orElseThrow()).map(LabelResponse::from).toList();
 }
 private void saveTags(String id,List<Long> ids){
    if(ids==null)return;
    var previous=tags(id).stream().map(LabelResponse::id).toList();
    var unique=ids.stream().distinct().toList();
    for(Long label:unique)if(!previous.contains(label) && labels.findById(label).filter(WorkLabel::isActive).isEmpty())
        throw new ApiException(400,"标签不存在或已停用，请重新选择");
    db.update("delete from personal_todo_labels where todo_id=?",id);
    for(int i=0;i<unique.size();i++)db.update("insert into personal_todo_labels(todo_id,label_id,sort_order) values(?,?,?)",id,unique.get(i),i);
 }
 private Todo get(String id,long owner){return db.query("select id,title,todo_date,completed from personal_todos where id=? and owner_id=?",(r,n)->new Todo(r.getString(1),r.getString(2),r.getDate(3).toLocalDate(),r.getBoolean(4),tags(r.getString(1))),id,owner).stream().findFirst().orElseThrow(()->new ApiException(404,"事项不存在"));}
 @GetMapping public List<Todo> list(){long owner=access.assertBusiness().getId();return db.query("select id,title,todo_date,completed from personal_todos where owner_id=? order by todo_date,id",(r,n)->new Todo(r.getString(1),r.getString(2),r.getDate(3).toLocalDate(),r.getBoolean(4),tags(r.getString(1))),owner);}
 @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public Todo create(@Valid @RequestBody Input input){long owner=access.assertBusiness().getId();String id=UUID.randomUUID().toString();db.update("insert into personal_todos(id,owner_id,title,todo_date,completed) values(?,?,?,?,?)",id,owner,input.title().trim(),input.date(),input.completed());saveTags(id,input.labelIds());return get(id,owner);}
 @PutMapping("/{id}") public Todo update(@PathVariable String id,@Valid @RequestBody Input input){long owner=access.assertBusiness().getId();if(db.update("update personal_todos set title=?,todo_date=?,completed=? where id=? and owner_id=?",input.title().trim(),input.date(),input.completed(),id,owner)!=1)throw new ApiException(404,"事项不存在");saveTags(id,input.labelIds());return get(id,owner);}
 @DeleteMapping("/{id}") public Todo delete(@PathVariable String id){long owner=access.assertBusiness().getId();var item=get(id,owner);db.update("delete from personal_todos where id=? and owner_id=?",id,owner);return item;}
}

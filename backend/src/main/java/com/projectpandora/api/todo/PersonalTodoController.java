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
@RestController @RequestMapping("/api/v1/personal-todos") @Transactional
public class PersonalTodoController {
 private final JdbcTemplate db;private final AccessService access;
 public PersonalTodoController(JdbcTemplate db,AccessService access){this.db=db;this.access=access;}
 public record Todo(String id,String title,LocalDate date,boolean completed){}
 public record Input(@NotBlank @Size(max=200) String title,@NotNull LocalDate date,boolean completed){}
 private Todo get(String id,long owner){return db.query("select id,title,todo_date,completed from personal_todos where id=? and owner_id=?",(r,n)->new Todo(r.getString(1),r.getString(2),r.getDate(3).toLocalDate(),r.getBoolean(4)),id,owner).stream().findFirst().orElseThrow(()->new ApiException(404,"事项不存在"));}
 @GetMapping public List<Todo> list(){long owner=access.assertBusiness().getId();return db.query("select id,title,todo_date,completed from personal_todos where owner_id=? order by todo_date,id",(r,n)->new Todo(r.getString(1),r.getString(2),r.getDate(3).toLocalDate(),r.getBoolean(4)),owner);}
 @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public Todo create(@Valid @RequestBody Input input){long owner=access.assertBusiness().getId();String id=UUID.randomUUID().toString();db.update("insert into personal_todos(id,owner_id,title,todo_date,completed) values(?,?,?,?,?)",id,owner,input.title().trim(),input.date(),input.completed());return get(id,owner);}
 @PutMapping("/{id}") public Todo update(@PathVariable String id,@Valid @RequestBody Input input){long owner=access.assertBusiness().getId();if(db.update("update personal_todos set title=?,todo_date=?,completed=? where id=? and owner_id=?",input.title().trim(),input.date(),input.completed(),id,owner)!=1)throw new ApiException(404,"事项不存在");return get(id,owner);}
 @DeleteMapping("/{id}") public Todo delete(@PathVariable String id){long owner=access.assertBusiness().getId();var item=get(id,owner);db.update("delete from personal_todos where id=? and owner_id=?",id,owner);return item;}
}

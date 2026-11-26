package com.projectpandora.api.todo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
 public record Todo(String id,String title,LocalDate date,boolean completed,List<LabelResponse> labels,
    LocalDateTime startAt,LocalDateTime endAt){}
 public record Snapshot(@NotBlank @Size(max=200) String title,@NotNull LocalDate date,boolean completed,
    @NotNull @Size(max=12) List<@NotNull Long> labelIds,
    @JsonFormat(lenient=OptBoolean.FALSE) LocalDateTime startAt,@JsonFormat(lenient=OptBoolean.FALSE) LocalDateTime endAt){}
 public static final class Input {
    @NotBlank @Size(max=200) private final String title;
    @NotNull private final LocalDate date;
    private final boolean completed;
    @Size(max=12) private final List<@NotNull Long> labelIds;
    @Valid private final Snapshot base;
    @Pattern(regexp="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private final String requestId;
    private LocalDateTime startAt,endAt;
    private boolean startSupplied,endSupplied;

    @JsonCreator public Input(@JsonProperty("title") String title,@JsonProperty("date") LocalDate date,
        @JsonProperty("completed") boolean completed,@JsonProperty("labelIds") List<Long> labelIds,
        @JsonProperty("base") Snapshot base,@JsonProperty("requestId") String requestId) {
        this.title=title;this.date=date;this.completed=completed;this.labelIds=labelIds;this.base=base;this.requestId=requestId;
    }

    // Legacy clients omit endpoints; explicit null from a scheduling client clears one.
    @JsonSetter @JsonFormat(lenient=OptBoolean.FALSE)
    public void setStartAt(LocalDateTime value) {startSupplied=true;startAt=value;}
    @JsonSetter @JsonFormat(lenient=OptBoolean.FALSE)
    public void setEndAt(LocalDateTime value) {endSupplied=true;endAt=value;}
 }
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
 private static final String SELECT_TODO="select id,title,todo_date,completed,start_at,end_at from personal_todos";
 private Todo readTodo(ResultSet row,boolean lock) throws SQLException {
    String id=row.getString("id");
    return new Todo(id,row.getString("title"),row.getObject("todo_date",LocalDate.class),row.getBoolean("completed"),
        tags(id,lock),row.getObject("start_at",LocalDateTime.class),row.getObject("end_at",LocalDateTime.class));
 }
 private Todo get(String id,long owner){return db.query(SELECT_TODO+" where id=? and owner_id=? for update",(r,n)->readTodo(r,true),id,owner).stream().findFirst().orElseThrow(()->new ApiException(404,"事项不存在"));}
 @GetMapping public List<Todo> list(){long owner=access.assertBusiness().getId();return db.query(SELECT_TODO+" where owner_id=? order by todo_date,id",(r,n)->readTodo(r,false),owner);}
 @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
 public Todo create(@Valid @RequestBody Input input){
    long owner=lockOwner();
    if(input.requestId!=null) {
        var saved=db.queryForList("select todo_id from personal_todo_requests where owner_id=? and request_id=? for update",String.class,owner,input.requestId);
        if(!saved.isEmpty()) {
            try {return get(saved.get(0),owner);}
            catch(ApiException e) {if(e.getStatus()!=404)throw e;throw new ApiException(409,"这条事项已创建过，后来被删除。要重新创建，请关闭后重新添加。");}
        }
    }
    validateSchedule(input.date,input.startAt,input.endAt,400);
    String id=UUID.randomUUID().toString();
    db.update("insert into personal_todos(id,owner_id,title,todo_date,completed,start_at,end_at) values(?,?,?,?,?,?,?)",
        id,owner,input.title.trim(),input.date,input.completed,input.startAt,input.endAt);
    saveTags(id,input.labelIds);
    if(input.requestId!=null)db.update("insert into personal_todo_requests(owner_id,request_id,todo_id) values(?,?,?)",owner,input.requestId,id);
    return get(id,owner);
 }
 @PutMapping("/{id}") public Todo update(@PathVariable String id,@Valid @RequestBody Input input){
    long owner=lockOwner();
    var latest=get(id,owner);
    String title=input.title.trim();
    LocalDate date=input.date;
    boolean completed=input.completed;
    var chosen=input.labelIds;
    var base=input.base;
    LocalDateTime start=input.startAt,end=input.endAt;
    if(base!=null){
        long baseOffset=ChronoUnit.DAYS.between(base.date(),date);
        validateSchedule(date,input.startSupplied?start:shift(base.startAt(),baseOffset),
            input.endSupplied?end:shift(base.endAt(),baseOffset),400);
        title=merge(base.title().trim(),title,latest.title());
        date=merge(base.date(),date,latest.date());
        completed=merge(base.completed(),completed,latest.completed());
        if(chosen!=null) chosen=merge(base.labelIds(),chosen,latest.labels().stream().map(LabelResponse::id).toList());
        if(input.startSupplied)start=merge(base.startAt(),start,latest.startAt());
        if(input.endSupplied)end=merge(base.endAt(),end,latest.endAt());
    }
    // A date change from a client that omits the schedule still moves the whole span.
    long offset=ChronoUnit.DAYS.between(latest.date(),date);
    if(!input.startSupplied)start=shift(latest.startAt(),offset);
    if(!input.endSupplied)end=shift(latest.endAt(),offset);
    validateSchedule(date,start,end,base==null?400:409);
    db.update("update personal_todos set title=?,todo_date=?,completed=?,start_at=?,end_at=? where id=? and owner_id=?",
        title,date,completed,start,end,id,owner);
    saveTags(id,chosen);
    return get(id,owner);
 }
 private static LocalDateTime shift(LocalDateTime value,long days){return value==null?null:value.plusDays(days);}
 private static void validateSchedule(LocalDate date,LocalDateTime start,LocalDateTime end,int status){
    if(start!=null && !start.toLocalDate().equals(date))throw new ApiException(status,"开始时间必须在事项日期当天");
    if(end!=null && (end.toLocalDate().isBefore(date) || (start!=null && end.isBefore(start))))
        throw new ApiException(status,"结束时间不能早于事项日期或开始时间");
    for(var time:new LocalDateTime[]{start,end})if(time!=null && (time.getYear()<1000 || time.getYear()>9999 || time.getNano()%1000!=0))
        throw new ApiException(status,"事项时间超出支持的范围或精度");
 }
 private static <T> T merge(T original,T proposed,T latest){
    if(Objects.equals(original,proposed))return latest;
    if(Objects.equals(original,latest) || Objects.equals(proposed,latest))return proposed;
    throw new ApiException(409,"这项内容已在其他设备修改，请对比后再保存。");
 }
 @DeleteMapping("/{id}") public Todo delete(@PathVariable String id){long owner=lockOwner();var item=get(id,owner);db.update("delete from personal_todos where id=? and owner_id=?",id,owner);return item;}
}

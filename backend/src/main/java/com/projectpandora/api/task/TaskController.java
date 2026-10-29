package com.projectpandora.api.task;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService tasks;
    public TaskController(TaskService tasks) { this.tasks = tasks; }
    @GetMapping
    public List<TaskResponse> list(@org.springframework.web.bind.annotation.RequestParam(required=false) Long userId) { return tasks.list(userId); }
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(201).body(tasks.create(request));
    }
    @PutMapping("/{id}")
    public TaskResponse edit(@PathVariable Long id,@Valid @RequestBody CreateTaskRequest input){return tasks.edit(id,input);}
    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public java.util.Map<String,String> delete(@PathVariable Long id){tasks.delete(id);return java.util.Map.of("message","任务已删除");}
    public record KeywordInput(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=24) String text) {}
    @PostMapping("/{id}/keywords")
    public java.util.Map<String,String> addKeyword(@PathVariable Long id,@Valid @RequestBody KeywordInput input){return java.util.Map.of("message",tasks.addKeyword(id,input.text()));}
    @PutMapping("/{id}/keywords/{keywordId}")
    public java.util.Map<String,String> editKeyword(@PathVariable Long id,@PathVariable Long keywordId,@Valid @RequestBody KeywordInput input){return java.util.Map.of("message",tasks.changeKeyword(id,keywordId,input.text(),false));}
    @org.springframework.web.bind.annotation.DeleteMapping("/{id}/keywords/{keywordId}")
    public java.util.Map<String,String> deleteKeyword(@PathVariable Long id,@PathVariable Long keywordId){return java.util.Map.of("message",tasks.changeKeyword(id,keywordId,"",true));}
    @PutMapping("/{id}/progress")
    public TaskResponse updateProgress(@PathVariable Long id, @Valid @RequestBody ProgressRequest request) {
        return tasks.updateProgress(id, request);
    }
}

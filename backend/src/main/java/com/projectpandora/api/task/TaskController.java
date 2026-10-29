package com.projectpandora.api.task;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService tasks;
    public TaskController(TaskService tasks) { this.tasks = tasks; }
    @GetMapping("/{id}")
    public TaskResponse detail(@PathVariable Long id) { return tasks.detail(id); }
    @GetMapping("/{id}/note")
    public TaskNoteResponse note(@PathVariable Long id) { return tasks.note(id); }
    public record NoteInput(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(max=2000) String content) {}
    @PutMapping("/{id}/note")
    public TaskNoteResponse saveNote(@PathVariable Long id,@Valid @RequestBody NoteInput input) { return tasks.saveNote(id,input.content()); }
    @GetMapping
    public List<TaskResponse> list(@org.springframework.web.bind.annotation.RequestParam(required=false) Long userId) { return tasks.list(userId); }
    @GetMapping("/pending-approvals")
    public List<TaskResponse> pendingApprovals() { return tasks.pendingApprovals(); }
    @PutMapping("/{id}/approval")
    public TaskResponse review(@PathVariable Long id, @Valid @RequestBody TaskApprovalRequest request) {
        return tasks.review(id, request);
    }
    @PostMapping("/{id}/withdraw")
    public TaskResponse withdraw(@PathVariable Long id) {return tasks.withdraw(id);}
    @GetMapping("/reviews")
    public List<TaskResponse> reviews() { return tasks.reviews(); }
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(201).body(tasks.create(request));
    }
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
    @PutMapping("/{id}")
    public TaskResponse requestChange(@PathVariable Long id, @Valid @RequestBody CreateTaskRequest request) {
        return tasks.requestChange(id, request);
    }
    @PostMapping("/{id}/approve")
    public TaskResponse approve(@PathVariable Long id, @RequestBody(required=false) ReviewRequest request) {
        return tasks.approve(id, request);
    }
    @PostMapping("/{id}/reject")
    public TaskResponse reject(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return tasks.reject(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tasks.delete(id);
        return ResponseEntity.noContent().build();
    }
}

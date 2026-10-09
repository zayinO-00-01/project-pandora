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
    public List<TaskResponse> list() { return tasks.list(); }
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(201).body(tasks.create(request));
    }
    @PutMapping("/{id}/progress")
    public TaskResponse updateProgress(@PathVariable Long id, @Valid @RequestBody ProgressRequest request) {
        return tasks.updateProgress(id, request);
    }
}
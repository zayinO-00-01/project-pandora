package com.projectpandora.api.task;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/task-reviews")
public class TaskReviewController {
    private final TaskService tasks;public TaskReviewController(TaskService tasks){this.tasks=tasks;}
    public record Decision(@NotNull @Pattern(regexp="approved|rejected|canceled") String status) {}
    @GetMapping public List<TaskService.ReviewResponse> list(){return tasks.reviews();}
    @PutMapping("/{id}") public TaskService.ReviewResponse decide(@PathVariable Long id,@Valid @RequestBody Decision input){return tasks.decide(id,input.status());}
}

package com.projectpandora.api.task;

import com.projectpandora.api.user.UserEntity;
import java.time.Instant;

public record TaskResponse(Long id, String title, String detail, String priority, String status,
        Instant dueAt, int progress, String progressNote, Long createdBy, Long assigneeId,
        String creatorName, String assigneeName, Instant createdAt, Instant updatedAt) {
    static TaskResponse from(TaskEntity task) {
        int progress = task.getProgress();
        return new TaskResponse(task.getId(), task.getTitle(), task.getDetail(), task.getPriority(),
                progress == 0 ? "todo" : progress == 100 ? "done" : "doing",
                task.getDueAt(), progress, task.getProgressNote(), task.getCreator().getId(), task.getAssignee().getId(),
                name(task.getCreator()), name(task.getAssignee()), task.getCreatedAt(), task.getUpdatedAt());
    }
    private static String name(UserEntity user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank() ? user.getUsername() : user.getDisplayName();
    }
}
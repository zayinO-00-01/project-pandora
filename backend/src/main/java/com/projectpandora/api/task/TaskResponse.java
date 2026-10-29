package com.projectpandora.api.task;

import com.projectpandora.api.user.UserEntity;
import java.time.Instant;

public record TaskResponse(Long id, String title, String detail, String priority, String status,
        Instant dueAt, Integer progress, String progressNote, Long createdBy, Long assigneeId,
        String creatorName, String assigneeName, Instant createdAt, Instant updatedAt, com.projectpandora.api.label.LabelResponse label,String approvalStatus, java.util.List<KeywordResponse> keywords) {
    public record KeywordResponse(Long id,String text,Long createdBy) {}
    static TaskResponse from(TaskEntity task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDetail(), task.getPriority(),
                task.getStatus(),
                task.getDueAt(), task.getProgress(), task.getProgressNote(), task.getCreator().getId(), task.getAssignee().getId(),
                name(task.getCreator()), name(task.getAssignee()), task.getCreatedAt(), task.getUpdatedAt(),com.projectpandora.api.label.LabelResponse.from(task.getLabel()),task.getApprovalStatus(),task.getKeywords().stream().map(k->new KeywordResponse(k.getId(),k.getText(),k.getCreatedBy())).toList());
    }
    private static String name(UserEntity user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank() ? user.getUsername() : user.getDisplayName();
    }
}

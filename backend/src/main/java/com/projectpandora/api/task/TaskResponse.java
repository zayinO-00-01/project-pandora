package com.projectpandora.api.task;

import com.projectpandora.api.user.UserEntity;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public record TaskResponse(Long id, String title, String detail, String priority, String status,
        Instant dueAt, Integer progress, String progressNote, Long createdBy, Long assigneeId,
        String creatorName, String assigneeName, Instant createdAt, Instant updatedAt,
        String approvalStatus, String approvalNote, Long reviewerId, Instant reviewedAt,
        String reviewStatus, String reviewReason, Long reviewedBy, Instant lifecycleReviewedAt,
        boolean deleted, String pendingChange, com.projectpandora.api.label.LabelResponse label, List<KeywordResponse> keywords, Instant startAt,
        List<Long> assigneeIds) {
    public record KeywordResponse(Long id,String text,Long createdBy) {}
    static TaskResponse from(TaskEntity task) {
        var assignees = task.getAssignees();
        return new TaskResponse(task.getId(), task.getTitle(), task.getDetail(), task.getPriority(),
                task.getStatus(),
                task.getDueAt(), task.getProgress(), task.getProgressNote(), task.getCreator().getId(), task.getAssignee().getId(),
                name(task.getCreator()), assignees.stream().map(TaskResponse::name).collect(Collectors.joining("、")), task.getCreatedAt(), task.getUpdatedAt(),
                task.getApprovalStatus(), task.getApprovalNote(), task.getReviewerId(), task.getReviewedAt(),
                task.getReviewStatus(), task.getReviewReason(), task.getReviewedBy(), task.getLifecycleReviewedAt(),
                task.isDeleted(), task.getPendingChange(),com.projectpandora.api.label.LabelResponse.from(task.getLabel()),
                task.getKeywords().stream().map(k->new KeywordResponse(k.getId(),k.getText(),k.getCreatedBy())).toList(), task.getStartAt(),
                assignees.stream().map(UserEntity::getId).toList());
    }
    private static String name(UserEntity user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank() ? user.getUsername() : user.getDisplayName();
    }
}

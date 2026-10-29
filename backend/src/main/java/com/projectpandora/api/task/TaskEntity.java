package com.projectpandora.api.task;

import com.projectpandora.api.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "dispatch_tasks")
public class TaskEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 128)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String detail;
    @Column(nullable = false, length = 16)
    private String priority;
    @Column(name = "due_at")
    private Instant dueAt;
    @Column(nullable = false, length = 16)
    private String status = "todo";
    private Integer progress;
    @Column(name = "progress_note", nullable = false, columnDefinition = "TEXT")
    private String progressNote = "";
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private UserEntity creator;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignee_id", nullable = false, updatable = false)
    private UserEntity assignee;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "approval_status", nullable = false, length = 16)
    private String approvalStatus = "approved";
    @Column(name = "approval_note", nullable = false, length = 500)
    private String approvalNote = "";
    @Column(name = "reviewer_id")
    private Long reviewerId;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected TaskEntity() {}

    TaskEntity(CreateTaskRequest request, UserEntity creator, UserEntity assignee) {
        title = request.title(); detail = request.detail(); priority = request.priority();
        dueAt = request.dueAt(); this.creator = creator; this.assignee = assignee;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
    void updateProgress(String state, Integer value, String note) { status = state; progress = value; progressNote = note; }

    void requireApproval() { approvalStatus = "pending"; }
    void review(String decision, String note, Long reviewer) {
        approvalStatus = decision; approvalNote = note; reviewerId = reviewer; reviewedAt = Instant.now();
    }
    public String getApprovalStatus() { return approvalStatus; }
    public String getApprovalNote() { return approvalNote; }
    public Long getReviewerId() { return reviewerId; }
    public Instant getReviewedAt() { return reviewedAt; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getPriority() { return priority; }
    public Instant getDueAt() { return dueAt; }
    public String getStatus() { return status; }
    public Integer getProgress() { return progress; }
    public String getProgressNote() { return progressNote; }
    public UserEntity getCreator() { return creator; }
    public UserEntity getAssignee() { return assignee; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

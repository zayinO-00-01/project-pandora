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
    @Column(nullable = false)
    private int progress;
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

    protected TaskEntity() {}

    TaskEntity(CreateTaskRequest request, UserEntity creator, UserEntity assignee) {
        title = request.title(); detail = request.detail(); priority = request.priority();
        dueAt = request.dueAt(); this.creator = creator; this.assignee = assignee;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
    void updateProgress(int value, String note) { progress = value; progressNote = note; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getPriority() { return priority; }
    public Instant getDueAt() { return dueAt; }
    public int getProgress() { return progress; }
    public String getProgressNote() { return progressNote; }
    public UserEntity getCreator() { return creator; }
    public UserEntity getAssignee() { return assignee; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
package com.projectpandora.api.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** Append-only audit trail for task lifecycle, reviews and deletions. */
@Entity
@Table(name = "task_events")
public class TaskEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "task_id", nullable = false)
    private Long taskId;
    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;
    @Column(name = "actor_id", nullable = false)
    private Long actorId;
    @Column(columnDefinition = "TEXT")
    private String note;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TaskEventEntity() {}

    TaskEventEntity(Long taskId, String eventType, Long actorId, String note) {
        this.taskId = taskId; this.eventType = eventType; this.actorId = actorId; this.note = note;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public String getEventType() { return eventType; }
    public Long getActorId() { return actorId; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}

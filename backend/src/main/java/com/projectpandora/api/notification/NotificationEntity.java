package com.projectpandora.api.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 500)
    private String body;
    @Column(name = "task_id")
    private Long taskId;
    @Column(name = "reference_id")
    private Long referenceId;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationEntity() {}

    NotificationEntity(Long userId,String type,String title,String body,Long taskId) {
        this(userId,type,title,body,taskId,null);
    }
    NotificationEntity(Long userId,String type,String title,String body,Long taskId,Long referenceId) {
        this.userId=userId;this.type=type;this.title=title;this.body=body;this.taskId=taskId;this.referenceId=referenceId;
    }

    @PrePersist void onCreate() {createdAt=Instant.now();}

    public Long getId() {return id;}
    public Long getUserId() {return userId;}
    public String getType() {return type;}
    public String getTitle() {return title;}
    public String getBody() {return body;}
    public Long getTaskId() {return taskId;}
    public Long getReferenceId() {return referenceId;}
    public Instant getReadAt() {return readAt;}
    public void setReadAt(Instant readAt) {this.readAt=readAt;}
    public Instant getCreatedAt() {return createdAt;}
    public boolean isRead() {return readAt!=null;}
}

package com.projectpandora.api.task;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="task_notes",uniqueConstraints=@UniqueConstraint(columnNames={"task_id","user_id"}))
public class TaskNote {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="task_id",nullable=false,updatable=false) private Long taskId;
    @Column(name="user_id",nullable=false,updatable=false) private Long userId;
    @Column(nullable=false,length=2000) private String content;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected TaskNote() {}
    TaskNote(Long taskId,Long userId) {this.taskId=taskId;this.userId=userId;}
    void update(String value) {content=value;updatedAt=Instant.now();}
    TaskNoteResponse response() {return new TaskNoteResponse(taskId,content,updatedAt);}
}

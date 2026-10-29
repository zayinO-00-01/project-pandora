package com.projectpandora.api.task;

import jakarta.persistence.*;

@Entity @Table(name="task_keywords")
public class TaskKeyword {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="task_id",nullable=false) private Long taskId;
    @Column(name="created_by",nullable=false) private Long createdBy;
    @Column(nullable=false,length=24) private String text;
    protected TaskKeyword() {}
    TaskKeyword(Long taskId,Long createdBy,String text){this.taskId=taskId;this.createdBy=createdBy;this.text=text;}
    public Long getId(){return id;}
    public Long getTaskId(){return taskId;}
    public Long getCreatedBy(){return createdBy;}
    public String getText(){return text;}
    void setText(String text){this.text=text;}
}

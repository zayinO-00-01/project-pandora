package com.projectpandora.api.task;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="task_reviews")
public class TaskReview {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="task_id",nullable=false) public Long taskId;
    @Column(name="applicant_id",nullable=false) public Long applicantId;
    @Column(name="reviewer_id",nullable=false) public Long reviewerId;
    @Column(nullable=false,length=16) public String operation;
    @Column(nullable=false,columnDefinition="TEXT") public String payload;
    @Column(nullable=false,length=16) public String status="pending";
    @Column(name="base_version",nullable=false) public long baseVersion;
    @Column(name="created_at",nullable=false) public Instant createdAt=Instant.now();
    @Column(name="decided_at") public Instant decidedAt;
}

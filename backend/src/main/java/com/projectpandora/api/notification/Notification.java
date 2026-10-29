package com.projectpandora.api.notification;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="notifications")
public class Notification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="recipient_id",nullable=false) public Long recipientId;
    @Column(nullable=false,length=128) public String title;
    @Column(nullable=false,length=1000) public String message;
    @Column(nullable=false,length=32) public String type;
    @Column(name="reference_id") public Long referenceId;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    @Column(name="read_at") public Instant readAt;
}

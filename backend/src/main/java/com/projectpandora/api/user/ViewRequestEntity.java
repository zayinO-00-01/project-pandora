package com.projectpandora.api.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="view_requests")
public class ViewRequestEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="requester_id",nullable=false) public Long requesterId;
    @Column(name="target_id",nullable=false) public Long targetId;
    @Column(nullable=false,length=500) public String reason;
    @Column(nullable=false,length=16) public String status;
    @Column(name="updated_at",nullable=false) public Instant updatedAt;
    @Version public long version;
}

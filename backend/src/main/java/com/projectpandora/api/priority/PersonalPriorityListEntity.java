package com.projectpandora.api.priority;

import jakarta.persistence.*;

@Entity
@Table(name = "personal_priority_lists")
public class PersonalPriorityListEntity {
    @Id
    @Column(name = "owner_id")
    Long ownerId;

    @Column(nullable = false)
    long revision;
}

package com.projectpandora.api.priority;

import jakarta.persistence.*;

@Entity
@Table(name = "personal_priorities")
public class PersonalPriorityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "owner_id", nullable = false)
    Long ownerId;

    @Column(nullable = false, length = 120)
    String title;

    @Column(length = 2000)
    String detail;

    @Column(name = "sort_order", nullable = false)
    Integer sortOrder;
}

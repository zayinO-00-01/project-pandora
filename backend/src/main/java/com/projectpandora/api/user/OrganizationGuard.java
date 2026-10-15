package com.projectpandora.api.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.stereotype.Component;

/** Serializes organization changes with request decisions using the same ordered database locks. */
@Component
public class OrganizationGuard {
    private final UserRepository users;
    private final EntityManager entityManager;
    public OrganizationGuard(UserRepository users,EntityManager entityManager) {
        this.users=users;this.entityManager=entityManager;
    }
    public List<UserEntity> lock() {
        var locked=users.lockOrganization();
        // Authentication may have loaded an account before this transaction acquired its locks.
        locked.forEach(u->entityManager.refresh(u,LockModeType.PESSIMISTIC_WRITE));
        return locked;
    }
}

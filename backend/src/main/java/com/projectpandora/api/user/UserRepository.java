package com.projectpandora.api.user;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    Optional<UserEntity> findByUsername(String username);
    List<UserEntity> findByManagerId(Long managerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u order by u.id")
    List<UserEntity> lockOrganization();
}

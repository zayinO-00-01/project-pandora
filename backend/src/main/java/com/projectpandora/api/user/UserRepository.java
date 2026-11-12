package com.projectpandora.api.user;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id=:id")
    Optional<UserEntity> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByPhone(String phone);
    Optional<UserEntity> findByIdCard(String idCard);
    List<UserEntity> findByManagerId(Long managerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u order by u.id")
    List<UserEntity> lockOrganization();
}

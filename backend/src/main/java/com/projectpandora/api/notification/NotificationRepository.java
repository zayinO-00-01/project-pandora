package com.projectpandora.api.notification;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity,Long> {
    List<NotificationEntity> findByUserIdOrderByCreatedAtDescIdDesc(Long userId,Pageable pageable);
    @Query("select count(n) from NotificationEntity n where n.userId=:userId and n.readAt is null")
    long countUnread(@Param("userId") Long userId);
    List<NotificationEntity> findByUserIdAndReadAtIsNull(Long userId);
}

package com.projectpandora.api.log;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkLogRepository extends JpaRepository<WorkLogEntity, Long> {
    List<WorkLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from WorkLogEntity l where l.id=:id and l.userId=:owner")
    Optional<WorkLogEntity> lockByOwnerAndId(@Param("owner") Long owner,@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from WorkLogEntity l where l.userId=:userId and l.logDate<=:date and coalesce(l.endDate,l.logDate)>=:date")
    List<WorkLogEntity> lockCoveringDate(@Param("userId") Long userId,@Param("date") LocalDate date);

    @Query("select l from WorkLogEntity l where l.userId=:userId and l.logDate<=:date and coalesce(l.endDate,l.logDate)>=:date order by l.createdAt desc")
    List<WorkLogEntity> coveringDate(@Param("userId") Long userId,@Param("date") LocalDate date);
}

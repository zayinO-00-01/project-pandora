package com.projectpandora.api.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewRequestRepository extends JpaRepository<ViewRequestEntity,Long> {
    Optional<ViewRequestEntity> findByRequesterIdAndTargetId(Long requesterId,Long targetId);
    List<ViewRequestEntity> findByRequesterIdOrTargetIdOrderByUpdatedAtDesc(Long requesterId,Long targetId);
    boolean existsByRequesterIdAndTargetIdAndStatus(Long requesterId,Long targetId,String status);
}

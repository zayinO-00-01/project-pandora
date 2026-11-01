package com.projectpandora.api.task;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskNoteRepository extends JpaRepository<TaskNote,Long> {
    Optional<TaskNote> findByTaskIdAndUserId(Long taskId,Long userId);
}

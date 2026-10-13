package com.projectpandora.api.log;

import java.time.Instant;
import java.time.LocalDate;

public record WorkLogResponse(Long id, Long userId, LocalDate logDate, String content,
                              String status, Instant createdAt, Instant updatedAt) {
    public static WorkLogResponse from(WorkLogEntity e) {
        return new WorkLogResponse(e.getId(), e.getUserId(), e.getLogDate(), e.getContent(),
                                  e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }
}

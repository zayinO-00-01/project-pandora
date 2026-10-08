package com.projectpandora.api.log;

import java.time.LocalDate;

public record WorkLogResponse(Long id, Long userId, LocalDate logDate, String content, String status) {
    public static WorkLogResponse from(WorkLogEntity entity) {
        return new WorkLogResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getLogDate(),
                entity.getContent(),
                entity.getStatus());
    }
}

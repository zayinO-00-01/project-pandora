package com.projectpandora.api.notification;

import java.time.Instant;

public record NotificationResponse(Long id,String type,String title,String body,Long taskId,boolean read,Instant createdAt) {
    static NotificationResponse from(NotificationEntity n) {
        return new NotificationResponse(n.getId(),n.getType(),n.getTitle(),n.getBody(),n.getTaskId(),n.isRead(),n.getCreatedAt());
    }
}

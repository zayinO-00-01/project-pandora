package com.projectpandora.api.notification;

import java.util.List;

public record NotificationList(List<NotificationResponse> items,long unreadCount) {}

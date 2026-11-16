package com.lifehub.notification.model.dto;

import java.time.Instant;
import java.util.UUID;

import com.lifehub.notification.model.entity.NotificationType;

/** A notification as the PWA shows it. {@code count} is above 1 for grouped shopping updates. */
public record NotificationDTO(UUID id, UUID workspaceId, NotificationType type, String title, String body,
        String link, int count, boolean read, Instant createdAt) {
}

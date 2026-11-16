package com.lifehub.notification.service;

import java.util.UUID;

import com.lifehub.notification.model.dto.NotificationDTO;

/** Published inside the transaction that stored a notification; streamed to the user once it commits. */
public record NotificationCreated(UUID userId, NotificationDTO notification) {
}

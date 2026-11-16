package com.lifehub.notification.model.dto;

import jakarta.validation.constraints.NotNull;

import com.lifehub.notification.model.entity.NotificationType;

/** Whether one type of notification is shown in the app and sent by email. */
public record NotificationPreferenceDTO(@NotNull NotificationType type, boolean inApp, boolean email) {
}

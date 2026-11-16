package com.lifehub.notification.model.dto;

import jakarta.validation.constraints.NotNull;

/** Marks a notification read ({@code true}) or unread ({@code false}). */
public record UpdateNotificationRequest(@NotNull Boolean read) {
}

package com.lifehub.notification.model.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The preferences to change. Types left out keep their current setting. */
public record UpdatePreferencesRequest(@NotNull @Size(max = 50) List<@Valid @NotNull NotificationPreferenceDTO> preferences) {
}

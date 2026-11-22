package com.lifehub.planner.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Attendees must be members of the workspace. {@code reminderMinutesBefore} is at most 4 weeks. The event cannot
 * start in the past. {@code recurrence} makes it repeat; leave it out for a one-off event.
 */
public record CreateCalendarEventRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @Size(max = 200) String location,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        boolean allDay,
        @PositiveOrZero @Max(40320) Integer reminderMinutesBefore,
        @Size(max = 50) List<@NotNull UUID> attendeeUserIds,
        @Valid RecurrenceDTO recurrence) {
}

package com.lifehub.planner.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Fields left out are unchanged. {@code attendeeUserIds} replaces the attendees. {@code clearReminder} removes
 * the reminder. {@code cancelled: true} cancels the event; a cancelled event cannot be changed.
 */
public record UpdateCalendarEventRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        @Size(max = 200) String location,
        Instant startsAt,
        Instant endsAt,
        Boolean allDay,
        @PositiveOrZero @Max(40320) Integer reminderMinutesBefore,
        Boolean clearReminder,
        @Size(max = 50) List<UUID> attendeeUserIds,
        Boolean cancelled) {
}

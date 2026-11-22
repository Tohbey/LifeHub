package com.lifehub.planner.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lifehub.planner.model.entity.EventStatus;

/**
 * An event, or one occurrence of a recurring event. In a list, {@code startsAt} and {@code endsAt} are the
 * occurrence's; {@code id} is always the series, which is what PATCH changes.
 *
 * @param recurrence how the event repeats, or null for a one-off event
 */
public record CalendarEventDTO(UUID id, String title, String description, String location, Instant startsAt,
        Instant endsAt, boolean allDay, Integer reminderMinutesBefore, EventStatus status, List<UUID> attendeeUserIds,
        UUID createdByUserId, RecurrenceDTO recurrence) {
}

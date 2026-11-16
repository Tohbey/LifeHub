package com.lifehub.planner.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lifehub.planner.model.entity.EventStatus;

public record CalendarEventDTO(UUID id, String title, String description, String location, Instant startsAt,
        Instant endsAt, boolean allDay, Integer reminderMinutesBefore, EventStatus status, List<UUID> attendeeUserIds,
        UUID createdByUserId) {
}

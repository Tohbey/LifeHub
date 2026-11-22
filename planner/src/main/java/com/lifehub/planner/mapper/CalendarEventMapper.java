package com.lifehub.planner.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lifehub.planner.model.dto.CalendarEventDTO;
import com.lifehub.planner.model.dto.RecurrenceDTO;
import com.lifehub.planner.model.entity.CalendarEvent;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CalendarEventMapper {

    /** Maps an event (the series, for a recurring one) and its attendees to a DTO. */
    public static CalendarEventDTO toDto(CalendarEvent e, List<UUID> attendees) {
        return toDto(e, attendees, e.getStartsAt(), e.getEndsAt());
    }

    /** Maps one occurrence of an event: the series' details with this occurrence's times. */
    public static CalendarEventDTO toDto(CalendarEvent e, List<UUID> attendees, Instant startsAt, Instant endsAt) {
        return new CalendarEventDTO(e.getId(), e.getTitle(), e.getDescription(), e.getLocation(), startsAt, endsAt,
                e.isAllDay(), e.getReminderMinutesBefore(), e.getStatus(), attendees, e.getCreatedByUserId(),
                recurrence(e));
    }

    /** The event's repeat rule as a DTO, or null for a one-off event. */
    public static RecurrenceDTO recurrence(CalendarEvent e) {
        return e.isRecurring()
                ? new RecurrenceDTO(e.getRecurrenceFrequency(), e.getRecurrenceInterval(), e.getRecurrenceUntil())
                : null;
    }
}

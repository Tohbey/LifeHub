package com.lifehub.planner.mapper;

import java.util.List;
import java.util.UUID;

import com.lifehub.planner.model.dto.CalendarEventDTO;
import com.lifehub.planner.model.entity.CalendarEvent;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CalendarEventMapper {

    /** Maps an event and its attendees to a DTO. */
    public static CalendarEventDTO toDto(CalendarEvent e, List<UUID> attendees) {
        return new CalendarEventDTO(e.getId(), e.getTitle(), e.getDescription(), e.getLocation(), e.getStartsAt(),
                e.getEndsAt(), e.isAllDay(), e.getReminderMinutesBefore(), e.getStatus(), attendees,
                e.getCreatedByUserId());
    }
}

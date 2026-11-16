package com.lifehub.planner.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.planner.model.dto.CalendarEventDTO;
import com.lifehub.planner.model.dto.CreateCalendarEventRequest;
import com.lifehub.planner.model.dto.UpdateCalendarEventRequest;
import com.lifehub.starter.web.PageResponse;

/**
 * A workspace's calendar. Every member can read and add events. Changing or cancelling one is for the person
 * who created it, or an owner or admin.
 */
public interface CalendarService {

    /** Pages through events overlapping [from, to); defaults to the next 31 days. */
    PageResponse<CalendarEventDTO> getEvents(UUID workspaceId, Instant from, Instant to, boolean includeCancelled,
            Pageable pageable);

    /** Returns one event. */
    CalendarEventDTO getEvent(UUID workspaceId, UUID eventId);

    /** Creates an event; publishes calendar.event.created. */
    CalendarEventDTO createEvent(UUID workspaceId, CreateCalendarEventRequest request);

    /** Changes or cancels an event; publishes calendar.event.updated or calendar.event.cancelled. */
    CalendarEventDTO updateEvent(UUID workspaceId, UUID eventId, UpdateCalendarEventRequest request);
}

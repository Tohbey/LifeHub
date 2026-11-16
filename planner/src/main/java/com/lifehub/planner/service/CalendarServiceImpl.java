package com.lifehub.planner.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.planner.mapper.CalendarEventMapper;
import com.lifehub.planner.model.dto.CalendarEventDTO;
import com.lifehub.planner.model.dto.CreateCalendarEventRequest;
import com.lifehub.planner.model.dto.UpdateCalendarEventRequest;
import com.lifehub.planner.model.entity.CalendarEvent;
import com.lifehub.planner.model.event.PlannerEvents;
import com.lifehub.planner.repository.AttendeeRepository;
import com.lifehub.planner.repository.CalendarEventRepository;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class CalendarServiceImpl implements CalendarService {

    static final Duration DEFAULT_RANGE = Duration.ofDays(31);
    static final Duration MAX_RANGE = Duration.ofDays(400);

    private final WorkspaceAccess access;
    private final CalendarEventRepository calendarEvents;
    private final AttendeeRepository attendees;
    private final DomainEventPublisher events;
    private final Clock clock;

    /** Checks membership, reads one page and loads the page's attendees in one query. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CalendarEventDTO> getEvents(UUID workspaceId, Instant from, Instant to,
            boolean includeCancelled, Pageable pageable) {
        access.require(workspaceId);
        Instant start = from != null ? from : clock.instant();
        Instant end = to != null ? to : start.plus(DEFAULT_RANGE);
        if (!start.isBefore(end) || Duration.between(start, end).compareTo(MAX_RANGE) > 0) {
            throw ApiException.badRequest("'from' must be before 'to', at most 400 days apart.");
        }
        Page<CalendarEvent> page = calendarEvents.findOverlapping(workspaceId, start, end, includeCancelled, pageable);
        Map<UUID, List<UUID>> byEvent = attendees.findAll(page.map(CalendarEvent::getId).getContent());
        return PageResponse.of(page, e -> CalendarEventMapper.toDto(e, byEvent.getOrDefault(e.getId(), List.of())));
    }

    /** Checks membership, then loads the event from this workspace only. */
    @Override
    @Transactional(readOnly = true)
    public CalendarEventDTO getEvent(UUID workspaceId, UUID eventId) {
        access.require(workspaceId);
        return CalendarEventMapper.toDto(load(workspaceId, eventId), attendees.find(eventId));
    }

    /** Checks the caller and attendees are members and the times make sense, then saves and publishes. */
    @Override
    @Transactional
    public CalendarEventDTO createEvent(UUID workspaceId, CreateCalendarEventRequest request) {
        Membership caller = access.require(workspaceId);
        requireValidTimes(request.startsAt(), request.endsAt());
        List<UUID> attendeeIds = distinct(request.attendeeUserIds());
        requireMembers(workspaceId, attendeeIds);

        CalendarEvent event = calendarEvents.save(new CalendarEvent(Ids.newId(), workspaceId, request.title().strip(),
                blankToNull(request.description()), blankToNull(request.location()), request.startsAt(),
                request.endsAt(), request.allDay(), request.reminderMinutesBefore(), caller.userId(), clock.instant()));
        attendees.replace(event.getId(), attendeeIds);

        events.publish(PlannerEvents.CALENDAR_EVENT_CREATED, workspaceId, caller.userId(),
                new PlannerEvents.CalendarEventCreated(event.getId(), event.getTitle(), event.getStartsAt(),
                        event.getEndsAt(), event.isAllDay(), attendeeIds));
        return CalendarEventMapper.toDto(event, attendeeIds);
    }

    /** Checks the caller may change it, applies the changes, and publishes what changed. */
    @Override
    @Transactional
    public CalendarEventDTO updateEvent(UUID workspaceId, UUID eventId, UpdateCalendarEventRequest request) {
        Membership caller = access.require(workspaceId);
        CalendarEvent event = load(workspaceId, eventId);
        if (!caller.userId().equals(event.getCreatedByUserId()) && !caller.role().isAtLeast(WorkspaceRole.ADMIN)) {
            throw ApiException.forbidden("Only the person who created this event, or an admin, can change it.");
        }
        if (!event.isActive()) {
            throw ApiException.conflict("This event is cancelled.");
        }
        Instant now = clock.instant();
        List<UUID> currentAttendees = attendees.find(eventId);

        if (Boolean.TRUE.equals(request.cancelled())) {
            event.cancel(now);
            events.publish(PlannerEvents.CALENDAR_EVENT_CANCELLED, workspaceId, caller.userId(),
                    new PlannerEvents.CalendarEventCancelled(eventId, event.getTitle(), event.getStartsAt()));
            return CalendarEventMapper.toDto(event, currentAttendees);
        }

        List<String> changed = new ArrayList<>();
        String title = request.title() != null ? request.title().strip() : event.getTitle();
        String description = request.description() != null ? blankToNull(request.description()) : event.getDescription();
        String location = request.location() != null ? blankToNull(request.location()) : event.getLocation();
        boolean allDay = request.allDay() != null ? request.allDay() : event.isAllDay();
        addIfChanged(changed, "title", event.getTitle(), title);
        addIfChanged(changed, "description", event.getDescription(), description);
        addIfChanged(changed, "location", event.getLocation(), location);
        addIfChanged(changed, "allDay", event.isAllDay(), allDay);
        event.describe(title, description, location, allDay, now);

        Instant startsAt = request.startsAt() != null ? request.startsAt() : event.getStartsAt();
        Instant endsAt = request.endsAt() != null ? request.endsAt() : event.getEndsAt();
        Integer reminder = Boolean.TRUE.equals(request.clearReminder()) ? null
                : request.reminderMinutesBefore() != null ? request.reminderMinutesBefore() : event.getReminderMinutesBefore();
        requireValidTimes(startsAt, endsAt);
        addIfChanged(changed, "startsAt", event.getStartsAt(), startsAt);
        addIfChanged(changed, "endsAt", event.getEndsAt(), endsAt);
        addIfChanged(changed, "reminderMinutesBefore", event.getReminderMinutesBefore(), reminder);
        event.schedule(startsAt, endsAt, reminder, now);

        List<UUID> attendeeIds = currentAttendees;
        if (request.attendeeUserIds() != null) {
            List<UUID> requested = distinct(request.attendeeUserIds());
            if (!new HashSet<>(requested).equals(new HashSet<>(currentAttendees))) {
                requireMembers(workspaceId, requested);
                attendees.replace(eventId, requested);
                attendeeIds = requested;
                changed.add("attendeeUserIds");
            }
        }
        if (!changed.isEmpty()) {
            events.publish(PlannerEvents.CALENDAR_EVENT_UPDATED, workspaceId, caller.userId(),
                    new PlannerEvents.CalendarEventUpdated(eventId, event.getTitle(), event.getStartsAt(),
                            event.getEndsAt(), attendeeIds, changed));
        }
        return CalendarEventMapper.toDto(event, attendeeIds);
    }

    /** Loads an event of this workspace, or 404. */
    private CalendarEvent load(UUID workspaceId, UUID eventId) {
        return calendarEvents.findByIdAndWorkspaceId(eventId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("Event not found."));
    }

    /** An event must end after it starts. */
    private static void requireValidTimes(Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw ApiException.badRequest("An event must end after it starts.");
        }
    }

    /** Every attendee must be an active member (one lookup). */
    private void requireMembers(UUID workspaceId, List<UUID> userIds) {
        if (!userIds.isEmpty()) {
            access.requireAllMembers(workspaceId, userIds);
        }
    }

    /** The IDs without duplicates, in order; null becomes empty. */
    private static List<UUID> distinct(List<UUID> ids) {
        return ids == null ? List.of() : ids.stream().distinct().toList();
    }

    /** Adds the field name when the value changed. */
    private static void addIfChanged(List<String> changed, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changed.add(field);
        }
    }

    /** Null for a blank string, otherwise the stripped string. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}

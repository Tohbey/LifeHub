package com.lifehub.planner.model.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lifehub.planner.model.dto.RecurrenceDTO;
import com.lifehub.starter.events.EventType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Events this service publishes. Must match deploy/docs/events.md (task.events and calendar.events). */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlannerEvents {

    public static final String TASK_TOPIC = "task.events";
    public static final String CALENDAR_TOPIC = "calendar.events";

    public static final EventType TASK_CREATED = new EventType(TASK_TOPIC, "task.created", 1);
    public static final EventType TASK_ASSIGNED = new EventType(TASK_TOPIC, "task.assigned", 1);
    public static final EventType TASK_COMPLETED = new EventType(TASK_TOPIC, "task.completed", 1);
    public static final EventType TASK_DELETED = new EventType(TASK_TOPIC, "task.deleted", 1);
    public static final EventType TASK_OVERDUE = new EventType(TASK_TOPIC, "task.overdue", 1);

    public static final EventType CALENDAR_EVENT_CREATED = new EventType(CALENDAR_TOPIC, "calendar.event.created", 1);
    public static final EventType CALENDAR_EVENT_UPDATED = new EventType(CALENDAR_TOPIC, "calendar.event.updated", 1);
    public static final EventType CALENDAR_EVENT_CANCELLED = new EventType(CALENDAR_TOPIC, "calendar.event.cancelled", 1);
    public static final EventType CALENDAR_EVENT_REMINDER_DUE =
            new EventType(CALENDAR_TOPIC, "calendar.event.reminder.due", 1);

    public record TaskCreated(UUID taskId, String title, UUID assigneeUserId, Instant dueAt) {
    }

    public record TaskAssigned(UUID taskId, String title, UUID assigneeUserId, UUID previousAssigneeUserId,
            Instant dueAt) {
    }

    public record TaskCompleted(UUID taskId, String title, UUID completedByUserId) {
    }

    public record TaskDeleted(UUID taskId, String title) {
    }

    public record TaskOverdue(UUID taskId, String title, UUID assigneeUserId, Instant dueAt) {
    }

    /** {@code recurrence} is null for a one-off event; for a series, {@code startsAt} is the first occurrence. */
    public record CalendarEventCreated(UUID calendarEventId, String title, Instant startsAt, Instant endsAt,
            boolean allDay, List<UUID> attendeeUserIds, RecurrenceDTO recurrence) {
    }

    public record CalendarEventUpdated(UUID calendarEventId, String title, Instant startsAt, Instant endsAt,
            List<UUID> attendeeUserIds, List<String> changedFields) {
    }

    public record CalendarEventCancelled(UUID calendarEventId, String title, Instant startsAt) {
    }

    public record CalendarEventReminderDue(UUID calendarEventId, String title, Instant startsAt,
            List<UUID> attendeeUserIds) {
    }
}

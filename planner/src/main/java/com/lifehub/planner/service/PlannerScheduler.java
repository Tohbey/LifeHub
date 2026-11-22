package com.lifehub.planner.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.planner.model.entity.CalendarEvent;
import com.lifehub.planner.model.entity.Task;
import com.lifehub.planner.model.event.PlannerEvents;
import com.lifehub.planner.repository.AttendeeRepository;
import com.lifehub.planner.repository.CalendarEventRepository;
import com.lifehub.planner.repository.TaskRepository;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.membership.WorkspaceTimezones;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Raises the time-based events planner owns (ADR 0008): task.overdue when an open task passes its due time,
 * and calendar.event.reminder.due when an event's reminder time arrives. Each is published once; rows are
 * locked with SKIP LOCKED so several planner instances never announce the same thing twice.
 */
@Component
@ConditionalOnProperty(name = "lifehub.planner.scheduler-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
class PlannerScheduler {

    private final TaskRepository tasks;
    private final CalendarEventRepository calendarEvents;
    private final AttendeeRepository attendees;
    private final DomainEventPublisher events;
    private final WorkspaceTimezones zones;
    private final Clock clock;

    /** Runs every minute: announces newly overdue tasks. */
    @Scheduled(fixedDelayString = "${lifehub.planner.scan-interval-ms:60000}")
    @Transactional
    public void announceOverdueTasks() {
        Instant now = clock.instant();
        for (Task task : tasks.lockNewlyOverdue(now)) {
            events.publish(PlannerEvents.TASK_OVERDUE, task.getWorkspaceId(), null, new PlannerEvents.TaskOverdue(
                    task.getId(), task.getTitle(), task.getAssigneeUserId(), task.getDueAt()));
            task.markOverdueNotified(now);
        }
    }

    /**
     * Runs every minute: announces due event reminders, one per occurrence. A recurring series then moves on to
     * its next occurrence; one whose occurrence passed unannounced (planner was down) skips to the next one
     * instead of sending a late reminder.
     */
    @Scheduled(fixedDelayString = "${lifehub.planner.scan-interval-ms:60000}")
    @Transactional
    public void announceDueReminders() {
        Instant now = clock.instant();
        for (CalendarEvent stale : calendarEvents.lockStaleSeries(now)) {
            advance(stale, now, now);
        }
        for (CalendarEvent event : calendarEvents.lockDueReminders(now)) {
            events.publish(PlannerEvents.CALENDAR_EVENT_REMINDER_DUE, event.getWorkspaceId(), null,
                    new PlannerEvents.CalendarEventReminderDue(event.getId(), event.getTitle(), event.getNextOccurrenceAt(),
                            attendees.find(event.getId())));
            if (event.isRecurring()) {
                advance(event, event.getNextOccurrenceAt().plusNanos(1), now);
            } else {
                event.markReminderSent(now);
            }
        }
    }

    /** Points a series' next reminder at its first occurrence starting at or after {@code from}, or ends it. */
    private void advance(CalendarEvent series, Instant from, Instant now) {
        RecurrenceCalculator.nextStartOnOrAfter(series.getStartsAt(), CalendarServiceImpl.rule(series),
                        zones.zoneOf(series.getWorkspaceId()), from)
                .ifPresentOrElse(series::moveReminderTo, () -> {
                    series.moveReminderTo(null);
                    series.markReminderSent(now);
                });
    }
}

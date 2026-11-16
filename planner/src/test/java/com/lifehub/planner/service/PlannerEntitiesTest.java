package com.lifehub.planner.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.lifehub.planner.model.entity.CalendarEvent;
import com.lifehub.planner.model.entity.Task;
import com.lifehub.planner.model.entity.TaskStatus;

class PlannerEntitiesTest {

    private final Instant now = Instant.parse("2026-11-16T10:00:00Z");

    @Test
    void aNewDueTimeOrReopeningCanBecomeOverdueAgain() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), "Bins", null, null, now, UUID.randomUUID(), now);
        task.markOverdueNotified(now);

        task.reschedule(now, now);
        assertThat(task.getOverdueNotifiedAt()).isEqualTo(now);

        task.reschedule(now.plusSeconds(3600), now);
        assertThat(task.getOverdueNotifiedAt()).isNull();

        task.markOverdueNotified(now);
        task.complete(UUID.randomUUID(), now);
        task.reopen(now);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(task.getOverdueNotifiedAt()).isNull();
    }

    @Test
    void theReminderTimeFollowsTheStartAndIsSentAgainWhenItMoves() {
        Instant start = Instant.parse("2026-11-25T17:30:00Z");
        CalendarEvent event = new CalendarEvent(UUID.randomUUID(), UUID.randomUUID(), "Parents' evening", null, null,
                start, start.plusSeconds(5400), false, 60, UUID.randomUUID(), now);
        assertThat(event.getRemindAt()).isEqualTo(Instant.parse("2026-11-25T16:30:00Z"));

        event.markReminderSent(now);
        event.schedule(start, start.plusSeconds(3600), 60, now);
        assertThat(event.getReminderSentAt()).as("same reminder time: not sent again").isEqualTo(now);

        event.schedule(start.plusSeconds(3600), start.plusSeconds(7200), 60, now);
        assertThat(event.getReminderSentAt()).isNull();

        event.schedule(start, start.plusSeconds(3600), null, now);
        assertThat(event.getRemindAt()).isNull();
    }
}

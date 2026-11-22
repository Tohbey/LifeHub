package com.lifehub.planner.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.lifehub.planner.model.entity.CalendarEvent;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    /** Finds an event only if it belongs to this workspace. */
    Optional<CalendarEvent> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Query("""
            select e from CalendarEvent e
            where e.workspaceId = :workspaceId
              and (:includeCancelled = true or e.status = com.lifehub.planner.model.entity.EventStatus.ACTIVE)
              and ((e.recurrenceFrequency is null and e.endsAt > :from and e.startsAt < :to)
                or (e.recurrenceFrequency is not null and e.startsAt < :to
                    and (e.recurrenceUntil is null or e.recurrenceUntil >= :fromDay)))
            order by e.startsAt
            """)
    /**
     * One-off events that overlap [from, to), and recurring series that may have an occurrence in it (they are
     * expanded in the service). {@code fromDay} is a day before {@code from}, to be safe across timezones.
     */
    List<CalendarEvent> findInRange(UUID workspaceId, Instant from, Instant to, LocalDate fromDay,
            boolean includeCancelled, Pageable limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select e from CalendarEvent e
            where e.status = com.lifehub.planner.model.entity.EventStatus.ACTIVE
              and e.reminderSentAt is null and e.remindAt <= :now and e.nextOccurrenceAt > :now
            order by e.remindAt
            limit 100
            """)
    /** Active events whose next reminder is due and not yet sent, for an occurrence still in the future, locked. */
    List<CalendarEvent> lockDueReminders(Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select e from CalendarEvent e
            where e.status = com.lifehub.planner.model.entity.EventStatus.ACTIVE
              and e.recurrenceFrequency is not null and e.nextOccurrenceAt <= :now
            order by e.nextOccurrenceAt
            limit 100
            """)
    /** Recurring series whose next occurrence has already passed (for example while planner was down), locked. */
    List<CalendarEvent> lockStaleSeries(Instant now);
}

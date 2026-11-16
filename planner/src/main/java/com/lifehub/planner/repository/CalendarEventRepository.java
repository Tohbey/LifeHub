package com.lifehub.planner.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.domain.Page;
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
            where e.workspaceId = :workspaceId and e.endsAt > :from and e.startsAt < :to
              and (:includeCancelled = true or e.status = com.lifehub.planner.model.entity.EventStatus.ACTIVE)
            """)
    /** Pages through events that overlap [from, to). */
    Page<CalendarEvent> findOverlapping(UUID workspaceId, Instant from, Instant to, boolean includeCancelled,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select e from CalendarEvent e
            where e.status = com.lifehub.planner.model.entity.EventStatus.ACTIVE
              and e.reminderSentAt is null and e.remindAt <= :now and e.startsAt > :now
            order by e.remindAt
            limit 100
            """)
    /** Active events whose reminder is due and not yet sent, still in the future, locked for this transaction. */
    List<CalendarEvent> lockDueReminders(Instant now);
}

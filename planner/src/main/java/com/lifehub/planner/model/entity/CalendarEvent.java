package com.lifehub.planner.model.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** An appointment in a workspace's calendar. Attendees are stored separately. */
@Entity
@Table(name = "calendar_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarEvent extends AssignedIdEntity {

    /** The workspace whose calendar it is in. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** What it is, for example "Parents' evening". */
    @Column(name = "title", nullable = false)
    private String title;

    /** Longer notes, or null. */
    @Column(name = "description")
    private String description;

    /** Where it is, or null. */
    @Column(name = "location")
    private String location;

    /** When it starts. */
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    /** When it ends. */
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    /** True for whole-day events. */
    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    /** How long before the start to remind attendees, or null for no reminder. */
    @Column(name = "reminder_minutes_before")
    private Integer reminderMinutesBefore;

    /** When the reminder is due, derived from the start and the reminder. */
    @Column(name = "remind_at")
    private Instant remindAt;

    /** When the reminder was published, or null. */
    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    /** Active or cancelled. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false)
    private EventStatus status;

    /** The member who created it. */
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    /** When it was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When it last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Creates an active event. */
    public CalendarEvent(UUID id, UUID workspaceId, String title, String description, String location, Instant startsAt,
            Instant endsAt, boolean allDay, Integer reminderMinutesBefore, UUID createdByUserId, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.allDay = allDay;
        this.status = EventStatus.ACTIVE;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
        schedule(startsAt, endsAt, reminderMinutesBefore, now);
    }

    /** Changes the text fields. */
    public void describe(String title, String description, String location, boolean allDay, Instant now) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.allDay = allDay;
        this.updatedAt = now;
    }

    /** Changes the time and reminder. A changed reminder time is sent again. */
    public void schedule(Instant startsAt, Instant endsAt, Integer reminderMinutesBefore, Instant now) {
        Instant newRemindAt = reminderMinutesBefore == null ? null
                : startsAt.minus(Duration.ofMinutes(reminderMinutesBefore));
        if (newRemindAt == null || !newRemindAt.equals(remindAt)) {
            this.reminderSentAt = null;
        }
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.reminderMinutesBefore = reminderMinutesBefore;
        this.remindAt = newRemindAt;
        this.updatedAt = now;
    }

    /** Cancels the event; no reminder is sent for it. */
    public void cancel(Instant now) {
        this.status = EventStatus.CANCELLED;
        this.updatedAt = now;
    }

    /** Records that the reminder was published. */
    public void markReminderSent(Instant now) {
        this.reminderSentAt = now;
    }

    /** True unless cancelled. */
    public boolean isActive() {
        return status == EventStatus.ACTIVE;
    }
}

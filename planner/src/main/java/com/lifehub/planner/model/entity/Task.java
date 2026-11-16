package com.lifehub.planner.model.entity;

import java.time.Instant;
import java.util.Objects;
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

/** A to-do in a workspace, optionally assigned to a member and due at a time. */
@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends AssignedIdEntity {

    /** The workspace it belongs to. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** Short description, for example "Pay electricity bill". */
    @Column(name = "title", nullable = false)
    private String title;

    /** Longer notes, or null. */
    @Column(name = "description")
    private String description;

    /** The member responsible, or null when unassigned. */
    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    /** When it should be done by, or null. */
    @Column(name = "due_at")
    private Instant dueAt;

    /** Open or done. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    /** When it was marked done, or null. */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** Who marked it done, or null. */
    @Column(name = "completed_by_user_id")
    private UUID completedByUserId;

    /** When task.overdue was published for the current due time, or null. */
    @Column(name = "overdue_notified_at")
    private Instant overdueNotifiedAt;

    /** The member who created it. */
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    /** When it was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When it last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Creates an open task. */
    public Task(UUID id, UUID workspaceId, String title, String description, UUID assigneeUserId, Instant dueAt,
            UUID createdByUserId, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.title = title;
        this.description = description;
        this.assigneeUserId = assigneeUserId;
        this.dueAt = dueAt;
        this.status = TaskStatus.OPEN;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Changes the text fields. */
    public void describe(String title, String description, Instant now) {
        this.title = title;
        this.description = description;
        this.updatedAt = now;
    }

    /** Assigns the task to a member, or unassigns it with null. */
    public void assign(UUID assigneeUserId, Instant now) {
        this.assigneeUserId = assigneeUserId;
        this.updatedAt = now;
    }

    /** Changes the due time; a new due time can become overdue again. */
    public void reschedule(Instant dueAt, Instant now) {
        if (!Objects.equals(this.dueAt, dueAt)) {
            this.overdueNotifiedAt = null;
        }
        this.dueAt = dueAt;
        this.updatedAt = now;
    }

    /** Marks the task done by this member. */
    public void complete(UUID userId, Instant now) {
        this.status = TaskStatus.DONE;
        this.completedAt = now;
        this.completedByUserId = userId;
        this.updatedAt = now;
    }

    /** Opens a done task again; it can become overdue again. */
    public void reopen(Instant now) {
        this.status = TaskStatus.OPEN;
        this.completedAt = null;
        this.completedByUserId = null;
        this.overdueNotifiedAt = null;
        this.updatedAt = now;
    }

    /** Records that task.overdue was published. */
    public void markOverdueNotified(Instant now) {
        this.overdueNotifiedAt = now;
    }
}

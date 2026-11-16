package com.lifehub.notification.model.entity;

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

/** One in-app notification for one user. */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends AssignedIdEntity {

    /** The LifeHub user the notification is for. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** The workspace it concerns, or null when it has none. */
    @Column(name = "workspace_id", updatable = false)
    private UUID workspaceId;

    /** What the notification is about. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, updatable = false)
    private NotificationType type;

    /** Short headline. */
    @Column(name = "title", nullable = false)
    private String title;

    /** One or two sentences of detail. */
    @Column(name = "body", nullable = false)
    private String body;

    /** Path inside the PWA to open, for example /planner/tasks/123, or null. */
    @Column(name = "link")
    private String link;

    /** Notifications with the same key and recipient are merged within the grouping window, or null. */
    @Column(name = "group_key", updatable = false)
    private String groupKey;

    /** How many updates this notification stands for (more than 1 once grouped). */
    @Column(name = "group_count", nullable = false)
    private int groupCount;

    /** The event that last created or updated this notification. */
    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    /** When the user read it, or null while unread. */
    @Column(name = "read_at")
    private Instant readAt;

    /** When it was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When it last changed (grouping, read state). */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Creates an unread notification. */
    public Notification(UUID id, UUID userId, UUID workspaceId, NotificationType type, String title, String body,
            String link, String groupKey, UUID sourceEventId, Instant now) {
        super(id);
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
        this.groupKey = groupKey;
        this.groupCount = 1;
        this.sourceEventId = sourceEventId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Folds one more update into a grouped notification and makes it unread again. */
    public void regroup(String title, String body, UUID sourceEventId, Instant now) {
        this.groupCount++;
        this.title = title;
        this.body = body;
        this.sourceEventId = sourceEventId;
        this.readAt = null;
        this.updatedAt = now;
    }

    /** Marks it read or unread. */
    public void markRead(boolean read, Instant now) {
        this.readAt = read ? (readAt == null ? now : readAt) : null;
        this.updatedAt = now;
    }

    /** True until the user has read it. */
    public boolean isUnread() {
        return readAt == null;
    }
}

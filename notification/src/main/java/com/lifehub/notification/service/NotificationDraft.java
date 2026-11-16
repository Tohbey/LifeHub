package com.lifehub.notification.service;

import java.util.UUID;

import com.lifehub.notification.model.entity.NotificationType;

/**
 * A notification to create for one recipient, before preferences are applied.
 *
 * @param groupKey         drafts with the same key for the same user are merged within the grouping window
 * @param groupedBodyFormat body used once merged; {@code %d} is replaced with the number of updates
 */
public record NotificationDraft(UUID userId, UUID workspaceId, NotificationType type, String title, String body,
        String link, String groupKey, String groupedBodyFormat) {

    /** A draft that is never grouped. */
    static NotificationDraft single(UUID userId, UUID workspaceId, NotificationType type, String title, String body,
            String link) {
        return new NotificationDraft(userId, workspaceId, type, title, body, link, null, null);
    }
}

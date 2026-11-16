package com.lifehub.notification.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.notification.model.dto.NotificationDTO;
import com.lifehub.notification.model.dto.UnreadCountDTO;
import com.lifehub.starter.web.PageResponse;

/** The caller's own notifications. Every method is scoped to the given user; there is no way to read others'. */
public interface NotificationService {

    /** Pages through the user's notifications, newest first, optionally only unread ones. */
    PageResponse<NotificationDTO> getNotifications(UUID userId, boolean unreadOnly, Pageable pageable);

    /** Marks one of the user's notifications read or unread. 404 if it is not theirs. */
    NotificationDTO markRead(UUID userId, UUID notificationId, boolean read);

    /** Marks all of the user's notifications read. */
    UnreadCountDTO markAllRead(UUID userId);

    /** How many of the user's notifications are unread. */
    UnreadCountDTO countUnread(UUID userId);
}

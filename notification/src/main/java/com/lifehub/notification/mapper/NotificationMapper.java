package com.lifehub.notification.mapper;

import com.lifehub.notification.model.dto.NotificationDTO;
import com.lifehub.notification.model.entity.Notification;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationMapper {

    /** Maps a notification entity to its DTO. */
    public static NotificationDTO toDto(Notification n) {
        return new NotificationDTO(n.getId(), n.getWorkspaceId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                n.getGroupCount(), !n.isUnread(), n.getCreatedAt());
    }
}

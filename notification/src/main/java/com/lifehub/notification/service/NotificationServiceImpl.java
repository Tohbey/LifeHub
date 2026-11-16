package com.lifehub.notification.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.notification.mapper.NotificationMapper;
import com.lifehub.notification.model.dto.NotificationDTO;
import com.lifehub.notification.model.dto.UnreadCountDTO;
import com.lifehub.notification.model.entity.Notification;
import com.lifehub.notification.repository.NotificationRepository;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notifications;
    private final Clock clock;

    /** Reads one page of the user's notifications. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationDTO> getNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        var page = unreadOnly
                ? notifications.findByUserIdAndReadAtIsNull(userId, pageable)
                : notifications.findByUserId(userId, pageable);
        return PageResponse.of(page, NotificationMapper::toDto);
    }

    /** Loads the notification only if it is the user's, then changes its read state. */
    @Override
    @Transactional
    public NotificationDTO markRead(UUID userId, UUID notificationId, boolean read) {
        Notification notification = notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> ApiException.notFound("Notification not found."));
        notification.markRead(read, clock.instant());
        return NotificationMapper.toDto(notification);
    }

    /** Marks everything read and returns the new unread count, which is 0. */
    @Override
    @Transactional
    public UnreadCountDTO markAllRead(UUID userId) {
        notifications.markAllRead(userId, clock.instant());
        return new UnreadCountDTO(0);
    }

    /** Counts unread notifications. */
    @Override
    @Transactional(readOnly = true)
    public UnreadCountDTO countUnread(UUID userId) {
        return new UnreadCountDTO(notifications.countByUserIdAndReadAtIsNull(userId));
    }
}

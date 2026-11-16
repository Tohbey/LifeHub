package com.lifehub.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.notification.config.NotificationProperties;
import com.lifehub.notification.mapper.NotificationMapper;
import com.lifehub.notification.model.dto.NotificationPreferenceDTO;
import com.lifehub.notification.model.entity.Channel;
import com.lifehub.notification.model.entity.Notification;
import com.lifehub.notification.model.entity.NotificationType;
import com.lifehub.notification.repository.DeliveryAttemptRepository;
import com.lifehub.notification.repository.DirectoryRepository;
import com.lifehub.notification.repository.NotificationRepository;
import com.lifehub.notification.repository.PreferenceRepository;
import com.lifehub.starter.events.EventEnvelope;
import com.lifehub.starter.events.ProcessedEvents;
import com.lifehub.starter.id.Ids;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/**
 * Handles one event end to end, in one transaction: skip duplicates, update the directory, work out the
 * notifications, apply each recipient's preferences, store them, and queue email deliveries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventDispatcher {

    static final String CONSUMER = "notification";

    private final ProcessedEvents processedEvents;
    private final DirectoryUpdater directoryUpdater;
    private final DirectoryRepository directory;
    private final NotificationRules rules;
    private final NotificationRepository notifications;
    private final PreferenceRepository preferences;
    private final DeliveryAttemptRepository deliveries;
    private final NotificationProperties properties;
    private final ApplicationEventPublisher applicationEvents;
    private final Clock clock;

    /** Processes the event once; a redelivered event is skipped. */
    @Transactional
    public void dispatch(EventEnvelope<JsonNode> event) {
        if (!processedEvents.markProcessed(event.eventId(), CONSUMER)) {
            log.debug("Skipping duplicate event {}", event.eventId());
            return;
        }
        directoryUpdater.apply(event);
        for (NotificationDraft draft : rules.draftsFor(event, directory)) {
            deliver(draft, event.eventId());
        }
    }

    /** Stores the draft for its recipient (or merges it into a recent group) and queues email if wanted. */
    private void deliver(NotificationDraft draft, UUID eventId) {
        Map<NotificationType, NotificationPreferenceDTO> prefs = preferences.findByUser(draft.userId());
        NotificationPreferenceDTO pref = prefs.getOrDefault(draft.type(), PreferenceDefaults.of(draft.type()));
        if (!pref.inApp() && !pref.email()) {
            return;
        }
        Instant now = clock.instant();
        Notification notification = draft.groupKey() == null ? null : notifications
                .findFirstByUserIdAndGroupKeyAndCreatedAtAfterOrderByCreatedAtDesc(draft.userId(), draft.groupKey(),
                        now.minus(properties.groupingWindow()))
                .orElse(null);
        boolean grouped = notification != null;
        if (grouped) {
            notification.regroup(draft.title(), draft.groupedBodyFormat().formatted(notification.getGroupCount() + 1),
                    eventId, now);
            if (!pref.inApp()) {
                notification.markRead(true, now);
            }
        } else {
            notification = notifications.save(new Notification(Ids.newId(), draft.userId(), draft.workspaceId(),
                    draft.type(), draft.title(), draft.body(), draft.link(), draft.groupKey(), eventId, now));
            if (!pref.inApp()) {
                // Email only: keep the row for the email's content, but do not show it as unread in the app.
                notification.markRead(true, now);
            }
        }
        if (pref.inApp()) {
            applicationEvents.publishEvent(new NotificationCreated(draft.userId(), NotificationMapper.toDto(notification)));
        }
        // A grouped update is not emailed again: one email per group is enough.
        if (pref.email() && properties.emailEnabled() && !grouped) {
            deliveries.queue(notification.getId(), Channel.EMAIL);
        }
    }
}

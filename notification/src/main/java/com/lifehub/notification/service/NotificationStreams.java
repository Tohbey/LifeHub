package com.lifehub.notification.service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.lifehub.notification.config.NotificationProperties;
import com.lifehub.starter.web.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Live notification streams (server-sent events), kept in memory per instance. A notification is pushed
 * only after the transaction that stored it commits. Clients reconnect when a stream times out, and read
 * the list endpoint to catch up on anything missed while disconnected.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationStreams {

    /** Streams are closed after this long so dead connections never pile up; the client reconnects. */
    static final Duration STREAM_TIMEOUT = Duration.ofMinutes(30);

    private final NotificationProperties properties;
    private final Map<UUID, List<SseEmitter>> streams = new ConcurrentHashMap<>();

    /** Opens a stream for the user. Fails with 429 when the user already has the maximum number open. */
    public SseEmitter open(UUID userId) {
        List<SseEmitter> userStreams = streams.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
        if (userStreams.size() >= properties.maxStreamsPerUser()) {
            throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Too many open notification streams. Close a tab and try again.");
        }
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT.toMillis());
        userStreams.add(emitter);
        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        send(userId, emitter, SseEmitter.event().comment("connected"));
        return emitter;
    }

    /** Pushes a stored notification to the user's open streams, once its transaction has committed. */
    @TransactionalEventListener
    public void onNotificationCreated(NotificationCreated created) {
        for (SseEmitter emitter : streams.getOrDefault(created.userId(), List.of())) {
            send(created.userId(), emitter, SseEmitter.event()
                    .name("notification")
                    .id(created.notification().id().toString())
                    .data(created.notification(), MediaType.APPLICATION_JSON));
        }
    }

    /** Sends a comment on every stream every 25 seconds, so proxies keep idle connections open. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        streams.forEach((userId, emitters) -> emitters.forEach(
                emitter -> send(userId, emitter, SseEmitter.event().comment("heartbeat"))));
    }

    /** How many streams are open in total, for tests and diagnostics. */
    int openStreams() {
        return streams.values().stream().mapToInt(List::size).sum();
    }

    /** Sends one event; a failed send means the client went away, so the stream is dropped. */
    private void send(UUID userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException ex) {
            log.debug("Dropping a closed notification stream", ex);
            remove(userId, emitter);
        }
    }

    /** Forgets a stream, and the user's entry when it was their last one. */
    private void remove(UUID userId, SseEmitter emitter) {
        streams.computeIfPresent(userId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}

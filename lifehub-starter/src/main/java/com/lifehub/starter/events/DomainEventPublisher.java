package com.lifehub.starter.events;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.starter.id.Ids;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Records a domain event in this service's {@code outbox} table, inside the caller's transaction. The
 * event reaches Kafka only if the business change commits. {@link OutboxRelay} does the sending. See ADR 0006.
 */
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    /** Publishes keyed by workspace, so events of one workspace stay in order. */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(EventType type, UUID workspaceId, UUID actorUserId, Object payload) {
        Objects.requireNonNull(workspaceId, "Events without a workspace need an explicit key");
        return publish(type, workspaceId, actorUserId, payload, workspaceId.toString());
    }

    /**
     * Writes the event to the outbox with an explicit Kafka key, for events with no workspace such as
     * user.created. Returns the new event's ID. Must run inside the caller's transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(EventType type, UUID workspaceId, UUID actorUserId, Object payload, String key) {
        UUID eventId = Ids.newId();
        Instant now = clock.instant();
        var envelope = new EventEnvelope<>(eventId, type.type(), type.version(), now, workspaceId, actorUserId, payload);
        jdbc.sql("""
                INSERT INTO outbox (id, topic, message_key, event_type, event_version, envelope, created_at)
                VALUES (:id, :topic, :key, :type, :version, :envelope, :createdAt)
                """)
                .param("id", Ids.toBytes(eventId))
                .param("topic", type.topic())
                .param("key", key)
                .param("type", type.type())
                .param("version", type.version())
                .param("envelope", jsonMapper.writeValueAsString(envelope))
                .param("createdAt", Dates.utc(now))
                .update();
        return eventId;
    }
}

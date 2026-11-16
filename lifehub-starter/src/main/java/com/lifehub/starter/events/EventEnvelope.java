package com.lifehub.starter.events;

import java.time.Instant;
import java.util.UUID;

/**
 * The envelope every LifeHub event uses on Kafka. Producers publish it with a typed payload; consumers
 * usually read it with a {@code JsonNode} payload and convert based on {@link #type()} and {@link #version()}.
 *
 * @param workspaceId null only for events with no workspace, such as {@code user.created}
 * @param actorUserId null for events raised by a scheduler
 */
public record EventEnvelope<T>(
        UUID eventId,
        String type,
        int version,
        Instant occurredAt,
        UUID workspaceId,
        UUID actorUserId,
        T payload) {
}

package com.lifehub.starter.events;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.starter.id.Ids;

import lombok.RequiredArgsConstructor;

/**
 * Makes a consumer idempotent. Call {@link #markProcessed} first, in the same transaction as the
 * consumer's own change, and skip the event if it returns false:
 *
 * <pre>{@code
 * @Transactional
 * void on(EventEnvelope<JsonNode> event) {
 *     if (!processedEvents.markProcessed(event.eventId(), "notification-tasks")) {
 *         return; // duplicate delivery
 *     }
 *     ...
 * }
 * }</pre>
 */
@RequiredArgsConstructor
public class ProcessedEvents {

    private final JdbcClient jdbc;
    private final Clock clock;

    /** Returns true the first time this consumer sees this event, false for a duplicate. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(UUID eventId, String consumer) {
        return jdbc.sql("""
                INSERT IGNORE INTO processed_events (event_id, consumer, processed_at)
                VALUES (:eventId, :consumer, :now)
                """)
                .param("eventId", Ids.toBytes(eventId))
                .param("consumer", consumer)
                .param("now", Dates.utc(clock.instant()))
                .update() == 1;
    }

    /** Removes records older than the topic retention, after which a duplicate can no longer arrive. */
    public int deleteOlderThan(Duration age) {
        return jdbc.sql("DELETE FROM processed_events WHERE processed_at < :cutoff")
                .param("cutoff", Dates.utc(clock.instant().minus(age)))
                .update();
    }
}

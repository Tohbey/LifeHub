package com.lifehub.notification.repository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.lifehub.notification.model.entity.Channel;
import com.lifehub.starter.persistence.Dates;
import com.lifehub.starter.id.Ids;

import lombok.RequiredArgsConstructor;

/** Deliveries over channels other than in-app, with their retry state. */
@Repository
@RequiredArgsConstructor
public class DeliveryAttemptRepository {

    private final JdbcClient jdbc;
    private final Clock clock;

    /** A pending email with everything needed to send it. */
    public record PendingEmail(UUID id, int attempts, String to, String title, String body, String link) {
    }

    /** Queues a delivery of this notification over this channel, due now. */
    public void queue(UUID notificationId, Channel channel) {
        Instant now = clock.instant();
        jdbc.sql("""
                INSERT INTO delivery_attempts (id, notification_id, channel, status, attempts, next_attempt_at, created_at)
                VALUES (:id, :notification, :channel, 'PENDING', 0, :now, :now)
                """)
                .param("id", Ids.toBytes(Ids.newId()))
                .param("notification", Ids.toBytes(notificationId))
                .param("channel", channel.name())
                .param("now", Dates.utc(now))
                .update();
    }

    /**
     * Claims due email deliveries, locking their rows until the transaction ends. SKIP LOCKED lets a second
     * instance take other rows. Deliveries whose recipient has no email any more are left out.
     */
    public List<PendingEmail> claimDueEmails(int limit) {
        return jdbc.sql("""
                SELECT d.id, d.attempts, r.email, n.title, n.body, n.link
                FROM delivery_attempts d
                JOIN notifications n ON n.id = d.notification_id
                JOIN directory_recipients r ON r.user_id = n.user_id
                WHERE d.channel = 'EMAIL' AND d.status = 'PENDING' AND d.next_attempt_at <= :now AND r.email IS NOT NULL
                ORDER BY d.next_attempt_at
                LIMIT :limit
                FOR UPDATE OF d SKIP LOCKED
                """)
                .param("now", Dates.utc(clock.instant()))
                .param("limit", limit)
                .query((rs, n) -> new PendingEmail(Ids.fromBytes(rs.getBytes(1)), rs.getInt(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)))
                .list();
    }

    /** Marks a delivery as sent. */
    public void markSent(UUID id) {
        jdbc.sql("UPDATE delivery_attempts SET status = 'SENT', attempts = attempts + 1, sent_at = :now, last_error = NULL WHERE id = :id")
                .param("id", Ids.toBytes(id))
                .param("now", Dates.utc(clock.instant()))
                .update();
    }

    /** Records a failure: retried at {@code retryAt}, or marked FAILED when {@code retryAt} is null. */
    public void markFailed(UUID id, String error, Instant retryAt) {
        jdbc.sql("""
                UPDATE delivery_attempts
                SET attempts = attempts + 1, last_error = :error,
                    status = CASE WHEN :retryAt IS NULL THEN 'FAILED' ELSE 'PENDING' END,
                    next_attempt_at = COALESCE(:retryAt, next_attempt_at)
                WHERE id = :id
                """)
                .param("id", Ids.toBytes(id))
                .param("error", error.length() > 1000 ? error.substring(0, 1000) : error)
                .param("retryAt", retryAt == null ? null : Dates.utc(retryAt))
                .update();
    }
}

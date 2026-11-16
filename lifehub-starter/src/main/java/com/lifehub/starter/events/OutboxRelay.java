package com.lifehub.starter.events;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

import com.lifehub.starter.persistence.Dates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends unsent outbox rows to Kafka in creation order and marks them sent once Kafka acknowledges.
 *
 * <p>Delivery is at least once: a crash between the acknowledgement and the commit sends the row again.
 * When a send fails the batch stops, so later events of the same workspace never overtake an earlier one.
 * A row that keeps failing blocks the outbox and shows up as old unsent rows, which is what to alert on.
 */
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private record Row(byte[] id, String topic, String key, String type, int version, String envelope) {
    }

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate transactions;
    private final OutboxProperties properties;
    private final Clock clock;

    /** Runs every poll interval: sends batches of unsent outbox rows until a batch comes back short. */
    @Scheduled(fixedDelayString = "${lifehub.outbox.poll-interval-ms:500}")
    public void relay() {
        Integer sent;
        do {
            sent = transactions.execute(status -> relayBatch());
        } while (sent != null && sent == properties.batchSize());
    }

    /** Sends one batch. Returns how many rows were sent. Must run inside a transaction. */
    int relayBatch() {
        // SKIP LOCKED lets a second instance of the service work on other rows instead of waiting.
        List<Row> rows = jdbc.sql("""
                SELECT id, topic, message_key, event_type, event_version, envelope
                FROM outbox
                WHERE sent_at IS NULL
                ORDER BY created_at, id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
                """)
                .param("limit", properties.batchSize())
                .query((rs, n) -> new Row(rs.getBytes(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), rs.getString(6)))
                .list();

        int sent = 0;
        for (Row row : rows) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(row.topic(), row.key(), row.envelope());
                record.headers().add("type", row.type().getBytes(StandardCharsets.UTF_8));
                record.headers().add("version", Integer.toString(row.version()).getBytes(StandardCharsets.UTF_8));
                kafka.send(record).get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
                jdbc.sql("UPDATE outbox SET sent_at = :now, attempts = attempts + 1 WHERE id = :id")
                        .param("now", Dates.utc(clock.instant()))
                        .param("id", row.id())
                        .update();
                sent++;
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Could not publish outbox event {} to {}, will retry", row.type(), row.topic(), ex);
                jdbc.sql("UPDATE outbox SET attempts = attempts + 1, last_error = :error WHERE id = :id")
                        .param("error", truncate(String.valueOf(ex.getMessage()), 1000))
                        .param("id", row.id())
                        .update();
                break;
            }
        }
        return sent;
    }

    /** Runs nightly: deletes sent outbox rows older than the retention period. */
    @Scheduled(cron = "${lifehub.outbox.cleanup-cron:0 15 3 * * *}")
    public void deleteOldSentRows() {
        int deleted = jdbc.sql("DELETE FROM outbox WHERE sent_at IS NOT NULL AND sent_at < :cutoff")
                .param("cutoff", Dates.utc(clock.instant().minus(properties.retention())))
                .update();
        if (deleted > 0) {
            log.info("Deleted {} sent outbox rows", deleted);
        }
    }

    /** Shortens a string to at most {@code max} characters, to fit the last_error column. */
    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}

package com.lifehub.audit.repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.lifehub.starter.events.EventEnvelope;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.persistence.Dates;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/** The append-only audit log. There is deliberately no update or delete. */
@Repository
@RequiredArgsConstructor
public class AuditLogRepository {

    private final JdbcClient jdbc;
    private final Clock clock;

    /** A stored entry, with the envelope still as text. */
    public record Row(UUID eventId, String type, int version, Instant occurredAt, UUID actorUserId, String envelope) {
    }

    /** Stores an event; returns false when it was already stored (redelivery). */
    public boolean append(String topic, EventEnvelope<JsonNode> event, String rawEnvelope) {
        return jdbc.sql("""
                INSERT IGNORE INTO audit_logs
                    (event_id, topic, event_type, event_version, workspace_id, actor_user_id, occurred_at, envelope, received_at)
                VALUES (:id, :topic, :type, :version, :workspace, :actor, :occurredAt, :envelope, :now)
                """)
                .param("id", Ids.toBytes(event.eventId()))
                .param("topic", topic)
                .param("type", event.type())
                .param("version", event.version())
                .param("workspace", event.workspaceId() == null ? null : Ids.toBytes(event.workspaceId()))
                .param("actor", event.actorUserId() == null ? null : Ids.toBytes(event.actorUserId()))
                .param("occurredAt", Dates.utc(event.occurredAt() != null ? event.occurredAt() : clock.instant()))
                .param("envelope", rawEnvelope)
                .param("now", Dates.utc(clock.instant()))
                .update() == 1;
    }

    /** One page of a workspace's entries, newest first, optionally of one type and within [from, to). */
    public List<Row> find(UUID workspaceId, String type, Instant from, Instant to, int limit, long offset) {
        return jdbc.sql("""
                SELECT event_id, event_type, event_version, occurred_at, actor_user_id, envelope FROM audit_logs
                WHERE workspace_id = :workspace
                  AND (:type IS NULL OR event_type = :type)
                  AND (:from IS NULL OR occurred_at >= :from)
                  AND (:to IS NULL OR occurred_at < :to)
                ORDER BY occurred_at DESC, event_id DESC
                LIMIT :limit OFFSET :offset
                """)
                .param("workspace", Ids.toBytes(workspaceId))
                .param("type", type)
                .param("from", from == null ? null : Dates.utc(from))
                .param("to", to == null ? null : Dates.utc(to))
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, n) -> new Row(Ids.fromBytes(rs.getBytes(1)), rs.getString(2), rs.getInt(3),
                        rs.getObject(4, LocalDateTime.class).toInstant(ZoneOffset.UTC),
                        rs.getBytes(5) == null ? null : Ids.fromBytes(rs.getBytes(5)), rs.getString(6)))
                .list();
    }

    /** How many entries match, for paging. */
    public long count(UUID workspaceId, String type, Instant from, Instant to) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM audit_logs
                WHERE workspace_id = :workspace
                  AND (:type IS NULL OR event_type = :type)
                  AND (:from IS NULL OR occurred_at >= :from)
                  AND (:to IS NULL OR occurred_at < :to)
                """)
                .param("workspace", Ids.toBytes(workspaceId))
                .param("type", type)
                .param("from", from == null ? null : Dates.utc(from))
                .param("to", to == null ? null : Dates.utc(to))
                .query(Long.class)
                .single();
    }
}

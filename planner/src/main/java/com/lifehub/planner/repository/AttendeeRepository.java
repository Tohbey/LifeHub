package com.lifehub.planner.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.lifehub.starter.id.Ids;

import lombok.RequiredArgsConstructor;

/** Who attends which calendar event. */
@Repository
@RequiredArgsConstructor
public class AttendeeRepository {

    private final JdbcClient jdbc;

    /** Replaces the attendees of an event. */
    public void replace(UUID eventId, Collection<UUID> userIds) {
        jdbc.sql("DELETE FROM calendar_event_attendees WHERE calendar_event_id = :event")
                .param("event", Ids.toBytes(eventId))
                .update();
        for (UUID userId : new LinkedHashSet<>(userIds)) {
            jdbc.sql("INSERT INTO calendar_event_attendees (calendar_event_id, user_id) VALUES (:event, :user)")
                    .param("event", Ids.toBytes(eventId))
                    .param("user", Ids.toBytes(userId))
                    .update();
        }
    }

    /** The attendees of one event. */
    public List<UUID> find(UUID eventId) {
        return findAll(List.of(eventId)).getOrDefault(eventId, List.of());
    }

    /** The attendees of several events, by event. */
    public Map<UUID, List<UUID>> findAll(Collection<UUID> eventIds) {
        Map<UUID, List<UUID>> result = new HashMap<>();
        if (eventIds.isEmpty()) {
            return result;
        }
        jdbc.sql("SELECT calendar_event_id, user_id FROM calendar_event_attendees WHERE calendar_event_id IN (:events)")
                .param("events", eventIds.stream().map(Ids::toBytes).toList())
                .query((rs, n) -> result.computeIfAbsent(Ids.fromBytes(rs.getBytes(1)), id -> new ArrayList<>())
                        .add(Ids.fromBytes(rs.getBytes(2))))
                .list();
        return result;
    }
}

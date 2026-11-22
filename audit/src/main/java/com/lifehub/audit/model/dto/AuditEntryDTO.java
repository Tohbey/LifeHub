package com.lifehub.audit.model.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

/**
 * One recorded event. {@code payload} is the event's payload exactly as it was published; {@code people} names
 * every user it mentions (actor included) whose name is known, so the UI can show names instead of IDs.
 *
 * @param actorName the actor's display name, or null for system events and users who never set a name
 */
public record AuditEntryDTO(UUID eventId, String type, int version, Instant occurredAt, UUID actorUserId,
        String actorName, JsonNode payload, Map<UUID, String> people) {
}

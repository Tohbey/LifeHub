package com.lifehub.audit.model.dto;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

/** One recorded event. {@code payload} is the event's payload exactly as it was published. */
public record AuditEntryDTO(UUID eventId, String type, int version, Instant occurredAt, UUID actorUserId,
        JsonNode payload) {
}

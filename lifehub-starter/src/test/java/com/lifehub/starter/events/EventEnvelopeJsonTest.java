package com.lifehub.starter.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The envelope's JSON shape is a contract with every consumer (deploy/docs/events.md). */
class EventEnvelopeJsonTest {

    private record TaskAssigned(UUID taskId, String title) {
    }

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void serialisesToTheDocumentedShape() {
        UUID eventId = UUID.fromString("0193a4b2-7c1e-7d3a-9f00-2b6c1e4d8a10");
        UUID workspaceId = UUID.fromString("0193a4a0-1b2c-7e4f-8a00-5d6e7f809a1b");
        UUID taskId = UUID.fromString("0193a4b2-6f00-7b1c-8d00-1a2b3c4d5e6f");
        var envelope = new EventEnvelope<>(eventId, "task.assigned", 1, Instant.parse("2026-11-11T09:15:00Z"),
                workspaceId, null, new TaskAssigned(taskId, "Pay electricity bill"));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(envelope));

        assertThat(json.get("eventId").asString()).isEqualTo(eventId.toString());
        assertThat(json.get("type").asString()).isEqualTo("task.assigned");
        assertThat(json.get("version").asInt()).isEqualTo(1);
        assertThat(json.get("occurredAt").asString()).isEqualTo("2026-11-11T09:15:00Z");
        assertThat(json.get("workspaceId").asString()).isEqualTo(workspaceId.toString());
        assertThat(json.get("actorUserId").isNull()).isTrue();
        assertThat(json.get("payload").get("taskId").asString()).isEqualTo(taskId.toString());
    }

    @Test
    void consumersCanReadItWithAGenericPayload() {
        String json = """
                {"eventId":"0193a4b2-7c1e-7d3a-9f00-2b6c1e4d8a10","type":"task.assigned","version":1,
                 "occurredAt":"2026-11-11T09:15:00Z","workspaceId":null,"actorUserId":null,
                 "payload":{"title":"x"},"someFutureField":true}
                """;

        EventEnvelope<?> envelope = mapper.readValue(json, EventEnvelope.class);

        assertThat(envelope.type()).isEqualTo("task.assigned");
        assertThat(envelope.occurredAt()).isEqualTo(Instant.parse("2026-11-11T09:15:00Z"));
    }
}

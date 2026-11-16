package com.lifehub.starter.events;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns a Kafka record value back into an {@link EventEnvelope}. Consumers read the envelope first, decide
 * from {@code type} and {@code version} whether they handle it, and only then convert the payload. Unknown
 * payload fields are ignored, so producers can add fields without breaking consumers.
 */
@RequiredArgsConstructor
public class EventReader {

    private static final TypeReference<EventEnvelope<JsonNode>> ENVELOPE = new TypeReference<>() {
    };

    private final JsonMapper jsonMapper;

    /** Parses a record value into an envelope with a JSON payload. Throws if it is not a valid envelope. */
    public EventEnvelope<JsonNode> read(String json) {
        EventEnvelope<JsonNode> envelope = jsonMapper.readValue(json, ENVELOPE);
        if (envelope.eventId() == null || envelope.type() == null) {
            throw new IllegalArgumentException("Not a LifeHub event envelope");
        }
        return envelope;
    }

    /** Converts an envelope's payload to the given record type. */
    public <T> T payload(EventEnvelope<JsonNode> envelope, Class<T> type) {
        return jsonMapper.treeToValue(envelope.payload(), type);
    }
}

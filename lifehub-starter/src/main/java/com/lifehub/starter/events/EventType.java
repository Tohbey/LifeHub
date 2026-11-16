package com.lifehub.starter.events;

/**
 * One entry of {@code deploy/docs/events.md}. Each service declares its event types as constants, for
 * example {@code new EventType("identity.events", "household.created", 1)}.
 */
public record EventType(String topic, String type, int version) {
}

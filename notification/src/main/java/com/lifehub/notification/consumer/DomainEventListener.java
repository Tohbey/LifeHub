package com.lifehub.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.lifehub.notification.service.EventDispatcher;
import com.lifehub.starter.events.EventReader;

import lombok.RequiredArgsConstructor;

/**
 * Reads every topic notification cares about (consumer group {@code notification}). A record that cannot be
 * handled is retried, then parked on {@code <topic>.dlt} by the starter's error handler.
 */
@Component
@RequiredArgsConstructor
class DomainEventListener {

    private final EventReader reader;
    private final EventDispatcher dispatcher;

    /** Parses the envelope and hands it to the dispatcher. */
    @KafkaListener(id = "notification", topics = {
            "identity.events", "finance.events", "task.events", "calendar.events", "shopping.events" })
    void onEvent(String value) {
        dispatcher.dispatch(reader.read(value));
    }
}

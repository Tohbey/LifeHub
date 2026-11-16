package com.lifehub.audit.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.lifehub.audit.service.AuditRecorder;

import lombok.RequiredArgsConstructor;

/** Reads every LifeHub topic (consumer group {@code audit}). Failures are retried, then parked on {@code <topic>.dlt}. */
@Component
@RequiredArgsConstructor
class AuditEventListener {

    private final AuditRecorder recorder;

    /** Appends the record to the audit log. */
    @KafkaListener(id = "audit", topics = {
            "identity.events", "finance.events", "task.events", "calendar.events", "shopping.events", "document.events" })
    void onEvent(ConsumerRecord<String, String> record) {
        recorder.record(record.topic(), record.value());
    }
}

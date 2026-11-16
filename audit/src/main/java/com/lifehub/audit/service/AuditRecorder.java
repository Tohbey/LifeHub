package com.lifehub.audit.service;

import org.springframework.stereotype.Service;

import com.lifehub.audit.repository.AuditLogRepository;
import com.lifehub.starter.events.EventReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Stores each consumed event in the audit log, once. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditRecorder {

    private final EventReader reader;
    private final AuditLogRepository auditLogs;

    /** Validates the envelope and appends it as received. A duplicate is ignored. */
    public void record(String topic, String value) {
        var event = reader.read(value);
        if (!auditLogs.append(topic, event, value)) {
            log.debug("Event {} is already in the audit log", event.eventId());
        }
    }
}

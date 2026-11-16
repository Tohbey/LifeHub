--liquibase formatted sql

-- Idempotent consumers (ADR 0006). Services that consume events include this file in their master changelog.

--changeset lifehub-starter:processed-events-1
CREATE TABLE processed_events (
    event_id     BINARY(16)   NOT NULL,
    consumer     VARCHAR(100) NOT NULL,
    processed_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (event_id, consumer),
    INDEX idx_processed_events_processed_at (processed_at)
);
--rollback DROP TABLE processed_events;

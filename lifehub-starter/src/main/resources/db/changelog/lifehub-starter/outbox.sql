--liquibase formatted sql

-- Transactional outbox (ADR 0006). Every service includes this file in its own master changelog.
-- Never edit an applied changeset here: add a new one, because every service schema has run it.

--changeset lifehub-starter:outbox-1
CREATE TABLE outbox (
    id            BINARY(16)    NOT NULL COMMENT 'Equals the envelope eventId',
    topic         VARCHAR(200)  NOT NULL,
    message_key   VARCHAR(64)   NOT NULL,
    event_type    VARCHAR(100)  NOT NULL,
    event_version INT           NOT NULL,
    envelope      JSON          NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    sent_at       DATETIME(6)   NULL,
    attempts      INT           NOT NULL DEFAULT 0,
    last_error    VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    INDEX idx_outbox_unsent (sent_at, created_at)
);
--rollback DROP TABLE outbox;

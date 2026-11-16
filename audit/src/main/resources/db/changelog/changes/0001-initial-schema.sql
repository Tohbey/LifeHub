--liquibase formatted sql

-- Append-only. The runtime database user gets SELECT and INSERT only (deploy/mysql/create-schemas.sh),
-- so not even a bug in this service can change or delete history.

--changeset audit:0001-1
CREATE TABLE audit_logs (
    event_id      BINARY(16)   NOT NULL COMMENT 'The envelope eventId: a redelivered event is stored once',
    topic         VARCHAR(200) NOT NULL,
    event_type    VARCHAR(100) NOT NULL,
    event_version INT          NOT NULL,
    workspace_id  BINARY(16)   NULL,
    actor_user_id BINARY(16)   NULL,
    occurred_at   DATETIME(6)  NOT NULL,
    envelope      JSON         NOT NULL COMMENT 'The event exactly as received',
    received_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (event_id),
    INDEX idx_audit_logs_workspace_occurred (workspace_id, occurred_at),
    INDEX idx_audit_logs_workspace_type (workspace_id, event_type, occurred_at)
);
--rollback DROP TABLE audit_logs;

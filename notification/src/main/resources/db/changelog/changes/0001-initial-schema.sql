--liquibase formatted sql

-- IDs are UUIDv7 in BINARY(16) (ADR 0004). Times are UTC in DATETIME(6).
-- directory_* tables are this service's copy of identity data, built from identity.events.

--changeset notification:0001-1
CREATE TABLE notifications (
    id              BINARY(16)    NOT NULL,
    user_id         BINARY(16)    NOT NULL,
    workspace_id    BINARY(16)    NULL,
    type            VARCHAR(50)   NOT NULL,
    title           VARCHAR(300)  NOT NULL,
    body            VARCHAR(1000) NOT NULL,
    link            VARCHAR(300)  NULL,
    group_key       VARCHAR(200)  NULL,
    group_count     INT           NOT NULL DEFAULT 1,
    source_event_id BINARY(16)    NOT NULL,
    read_at         DATETIME(6)   NULL,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notifications_user_created (user_id, created_at),
    INDEX idx_notifications_user_unread (user_id, read_at),
    INDEX idx_notifications_user_group (user_id, group_key, created_at)
);
--rollback DROP TABLE notifications;

--changeset notification:0001-2
CREATE TABLE notification_preferences (
    user_id    BINARY(16)  NOT NULL,
    type       VARCHAR(50) NOT NULL,
    in_app     BOOLEAN     NOT NULL,
    email      BOOLEAN     NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, type)
);
--rollback DROP TABLE notification_preferences;

--changeset notification:0001-3
CREATE TABLE delivery_attempts (
    id              BINARY(16)    NOT NULL,
    notification_id BINARY(16)    NOT NULL,
    channel         VARCHAR(20)   NOT NULL COMMENT 'EMAIL (PUSH in phase 4)',
    status          VARCHAR(20)   NOT NULL COMMENT 'PENDING, SENT or FAILED',
    attempts        INT           NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6)   NOT NULL,
    last_error      VARCHAR(1000) NULL,
    sent_at         DATETIME(6)   NULL,
    created_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_delivery_attempts_due (status, next_attempt_at),
    CONSTRAINT fk_delivery_attempts_notification FOREIGN KEY (notification_id) REFERENCES notifications (id)
);
--rollback DROP TABLE delivery_attempts;

--changeset notification:0001-4
CREATE TABLE directory_workspaces (
    id         BINARY(16)   NOT NULL,
    name       VARCHAR(100) NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);
--rollback DROP TABLE directory_workspaces;

--changeset notification:0001-5
CREATE TABLE directory_members (
    workspace_id BINARY(16)  NOT NULL,
    user_id      BINARY(16)  NOT NULL,
    role         VARCHAR(20) NOT NULL,
    active       BOOLEAN     NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (workspace_id, user_id)
);
--rollback DROP TABLE directory_members;

--changeset notification:0001-6
CREATE TABLE directory_recipients (
    user_id      BINARY(16)   NOT NULL,
    display_name VARCHAR(100) NULL,
    email        VARCHAR(254) NULL COMMENT 'Unverified profile email',
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id)
);
--rollback DROP TABLE directory_recipients;

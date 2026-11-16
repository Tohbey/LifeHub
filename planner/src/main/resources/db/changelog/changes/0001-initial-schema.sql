--liquibase formatted sql

-- IDs are UUIDv7 in BINARY(16) (ADR 0004). Times are UTC in DATETIME(6).
-- workspace_id and user IDs are plain values owned by identity-workspace: no foreign keys to them.

--changeset planner:0001-1
CREATE TABLE tasks (
    id                   BINARY(16)    NOT NULL,
    workspace_id         BINARY(16)    NOT NULL,
    title                VARCHAR(200)  NOT NULL,
    description          VARCHAR(2000) NULL,
    assignee_user_id     BINARY(16)    NULL,
    due_at               DATETIME(6)   NULL,
    status               VARCHAR(10)   NOT NULL COMMENT 'OPEN or DONE',
    completed_at         DATETIME(6)   NULL,
    completed_by_user_id BINARY(16)    NULL,
    overdue_notified_at  DATETIME(6)   NULL COMMENT 'Set when task.overdue was published for the current due time',
    created_by_user_id   BINARY(16)    NOT NULL,
    created_at           DATETIME(6)   NOT NULL,
    updated_at           DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_tasks_workspace_status_due (workspace_id, status, due_at),
    INDEX idx_tasks_overdue (status, overdue_notified_at, due_at)
);
--rollback DROP TABLE tasks;

--changeset planner:0001-2
CREATE TABLE calendar_events (
    id                      BINARY(16)    NOT NULL,
    workspace_id            BINARY(16)    NOT NULL,
    title                   VARCHAR(200)  NOT NULL,
    description             VARCHAR(2000) NULL,
    location                VARCHAR(200)  NULL,
    starts_at               DATETIME(6)   NOT NULL,
    ends_at                 DATETIME(6)   NOT NULL,
    all_day                 BOOLEAN       NOT NULL,
    reminder_minutes_before INT           NULL,
    remind_at               DATETIME(6)   NULL COMMENT 'starts_at minus the reminder, or null for no reminder',
    reminder_sent_at        DATETIME(6)   NULL,
    status                  VARCHAR(10)   NOT NULL COMMENT 'ACTIVE or CANCELLED',
    created_by_user_id      BINARY(16)    NOT NULL,
    created_at              DATETIME(6)   NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_calendar_events_workspace_starts (workspace_id, starts_at),
    INDEX idx_calendar_events_reminders (status, reminder_sent_at, remind_at)
);
--rollback DROP TABLE calendar_events;

--changeset planner:0001-3
CREATE TABLE calendar_event_attendees (
    calendar_event_id BINARY(16) NOT NULL,
    user_id           BINARY(16) NOT NULL,
    PRIMARY KEY (calendar_event_id, user_id),
    CONSTRAINT fk_attendees_calendar_event FOREIGN KEY (calendar_event_id) REFERENCES calendar_events (id)
);
--rollback DROP TABLE calendar_event_attendees;

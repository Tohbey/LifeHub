--liquibase formatted sql

-- Recurring calendar events. A series is one row; its occurrences are worked out from the rule in the
-- workspace's timezone. next_occurrence_at is the occurrence the next reminder is for.

--changeset planner:0002-1
ALTER TABLE calendar_events
    ADD COLUMN recurrence_frequency VARCHAR(10) NULL COMMENT 'DAILY, WEEKLY, MONTHLY or YEARLY; null for a one-off event',
    ADD COLUMN recurrence_interval  INT         NULL COMMENT 'Every N days, weeks, months or years',
    ADD COLUMN recurrence_until     DATE        NULL COMMENT 'Last day (local) an occurrence may start, or null for no end',
    ADD COLUMN next_occurrence_at   DATETIME(6) NULL COMMENT 'Start of the occurrence the next reminder is for';
--rollback ALTER TABLE calendar_events DROP COLUMN recurrence_frequency, DROP COLUMN recurrence_interval, DROP COLUMN recurrence_until, DROP COLUMN next_occurrence_at;

--changeset planner:0002-2
UPDATE calendar_events SET next_occurrence_at = starts_at WHERE next_occurrence_at IS NULL;
--rollback SELECT 1;

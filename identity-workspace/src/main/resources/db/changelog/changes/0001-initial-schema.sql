--liquibase formatted sql

-- IDs are UUIDv7 in BINARY(16) (ADR 0004). Times are UTC in DATETIME(6).
-- Foreign keys are fine here: every table belongs to this service's own schema.

--changeset identity-workspace:0001-1
CREATE TABLE users (
    id            BINARY(16)   NOT NULL,
    logto_subject VARCHAR(191) NOT NULL,
    display_name  VARCHAR(100) NULL,
    email         VARCHAR(254) NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_logto_subject (logto_subject),
    INDEX idx_users_email (email)
);
--rollback DROP TABLE users;

--changeset identity-workspace:0001-2
CREATE TABLE workspaces (
    id         BINARY(16)   NOT NULL,
    type       VARCHAR(20)  NOT NULL COMMENT 'PERSONAL or HOUSEHOLD',
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);
--rollback DROP TABLE workspaces;

--changeset identity-workspace:0001-3
CREATE TABLE households (
    id                 BINARY(16)  NOT NULL COMMENT 'Same value as the household workspace id',
    default_currency   VARCHAR(3)  NOT NULL,
    timezone           VARCHAR(64) NOT NULL,
    created_by_user_id BINARY(16)  NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_households_workspace FOREIGN KEY (id) REFERENCES workspaces (id),
    CONSTRAINT fk_households_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);
--rollback DROP TABLE households;

--changeset identity-workspace:0001-4
CREATE TABLE workspace_members (
    id           BINARY(16)  NOT NULL,
    workspace_id BINARY(16)  NOT NULL,
    user_id      BINARY(16)  NOT NULL,
    role         VARCHAR(20) NOT NULL COMMENT 'OWNER, ADMIN or MEMBER',
    status       VARCHAR(20) NOT NULL COMMENT 'ACTIVE or REMOVED',
    joined_at    DATETIME(6) NOT NULL,
    removed_at   DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workspace_members_workspace_user (workspace_id, user_id),
    INDEX idx_workspace_members_user_status (user_id, status),
    CONSTRAINT fk_workspace_members_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_workspace_members_user FOREIGN KEY (user_id) REFERENCES users (id)
);
--rollback DROP TABLE workspace_members;

--changeset identity-workspace:0001-5
CREATE TABLE household_invitations (
    id                  BINARY(16)   NOT NULL,
    household_id        BINARY(16)   NOT NULL,
    email               VARCHAR(254) NOT NULL,
    role                VARCHAR(20)  NOT NULL,
    token_hash          VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hex of the invite token',
    invited_by_user_id  BINARY(16)   NOT NULL,
    status              VARCHAR(20)  NOT NULL COMMENT 'PENDING or ACCEPTED',
    expires_at          DATETIME(6)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    accepted_by_user_id BINARY(16)   NULL,
    accepted_at         DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_household_invitations_token_hash (token_hash),
    INDEX idx_household_invitations_household (household_id),
    CONSTRAINT fk_household_invitations_household FOREIGN KEY (household_id) REFERENCES households (id),
    CONSTRAINT fk_household_invitations_invited_by FOREIGN KEY (invited_by_user_id) REFERENCES users (id)
);
--rollback DROP TABLE household_invitations;

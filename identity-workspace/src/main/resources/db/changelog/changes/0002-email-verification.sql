--liquibase formatted sql

-- Email verification: a profile email is only used for mail and for matching invitations once its owner has
-- opened a one-time link sent to it. Only the SHA-256 hash of the link's token is stored.

--changeset identity-workspace:0002-1
ALTER TABLE users ADD COLUMN email_verified_at DATETIME(6) NULL COMMENT 'When the current email was verified, or null';
--rollback ALTER TABLE users DROP COLUMN email_verified_at;

--changeset identity-workspace:0002-2
CREATE TABLE email_verifications (
    id         BINARY(16)   NOT NULL,
    user_id    BINARY(16)   NOT NULL,
    email      VARCHAR(254) NOT NULL COMMENT 'The address the link was sent to; must still be the profile email when used',
    token_hash CHAR(64)     NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    used_at    DATETIME(6)  NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verifications_token_hash (token_hash),
    INDEX idx_email_verifications_user (user_id, created_at),
    CONSTRAINT fk_email_verifications_user FOREIGN KEY (user_id) REFERENCES users (id)
);
--rollback DROP TABLE email_verifications;

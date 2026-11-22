--liquibase formatted sql

-- IDs are UUIDv7 in BINARY(16) (ADR 0004). Times are UTC in DATETIME(6).
-- visibility: PERSONAL (only its author sees it) or SHARED (every member of the workspace). Tags are lower case.

--changeset notes:0001-1
CREATE TABLE notes (
    id                 BINARY(16)   NOT NULL,
    workspace_id       BINARY(16)   NOT NULL,
    title              VARCHAR(150) NOT NULL,
    body               TEXT         NULL,
    visibility         VARCHAR(10)  NOT NULL,
    pinned             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by_user_id BINARY(16)   NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notes_workspace (workspace_id, visibility, created_by_user_id, updated_at)
);
--rollback DROP TABLE notes;

--changeset notes:0001-2
CREATE TABLE note_tags (
    note_id BINARY(16)  NOT NULL,
    tag     VARCHAR(40) NOT NULL,
    PRIMARY KEY (note_id, tag),
    INDEX idx_note_tags_tag (tag),
    CONSTRAINT fk_note_tags_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE
);
--rollback DROP TABLE note_tags;

--changeset notes:0001-3
CREATE TABLE bookmarks (
    id                 BINARY(16)    NOT NULL,
    workspace_id       BINARY(16)    NOT NULL,
    url                VARCHAR(2000) NOT NULL COMMENT 'http or https only',
    title              VARCHAR(200)  NOT NULL,
    description        VARCHAR(500)  NULL,
    visibility         VARCHAR(10)   NOT NULL,
    created_by_user_id BINARY(16)    NOT NULL,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_bookmarks_workspace (workspace_id, visibility, created_by_user_id, created_at)
);
--rollback DROP TABLE bookmarks;

--changeset notes:0001-4
CREATE TABLE bookmark_tags (
    bookmark_id BINARY(16)  NOT NULL,
    tag         VARCHAR(40) NOT NULL,
    PRIMARY KEY (bookmark_id, tag),
    INDEX idx_bookmark_tags_tag (tag),
    CONSTRAINT fk_bookmark_tags_bookmark FOREIGN KEY (bookmark_id) REFERENCES bookmarks (id) ON DELETE CASCADE
);
--rollback DROP TABLE bookmark_tags;

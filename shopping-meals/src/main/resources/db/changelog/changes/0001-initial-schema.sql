--liquibase formatted sql

-- IDs are UUIDv7 in BINARY(16) (ADR 0004). Times are UTC in DATETIME(6).
-- Recipes and meal plans come in phase 3.

--changeset shopping-meals:0001-1
CREATE TABLE shopping_lists (
    id                 BINARY(16)   NOT NULL,
    workspace_id       BINARY(16)   NOT NULL,
    name               VARCHAR(100) NOT NULL,
    archived_at        DATETIME(6)  NULL,
    created_by_user_id BINARY(16)   NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_shopping_lists_workspace (workspace_id, archived_at)
);
--rollback DROP TABLE shopping_lists;

--changeset shopping-meals:0001-2
CREATE TABLE shopping_items (
    id                   BINARY(16)   NOT NULL,
    shopping_list_id     BINARY(16)   NOT NULL,
    workspace_id         BINARY(16)   NOT NULL COMMENT 'Copied from the list, for the membership check',
    name                 VARCHAR(120) NOT NULL,
    quantity             VARCHAR(40)  NULL COMMENT 'Free text: 2, 500 g, a bunch',
    note                 VARCHAR(300) NULL,
    purchased_at         DATETIME(6)  NULL,
    purchased_by_user_id BINARY(16)   NULL,
    added_by_user_id     BINARY(16)   NOT NULL,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_shopping_items_list (shopping_list_id, purchased_at, created_at),
    CONSTRAINT fk_shopping_items_list FOREIGN KEY (shopping_list_id) REFERENCES shopping_lists (id)
);
--rollback DROP TABLE shopping_items;

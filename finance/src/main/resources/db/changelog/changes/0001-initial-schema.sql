--liquibase formatted sql

-- IDs are UUIDv7 in BINARY(16) (ADR 0004). Times are UTC in DATETIME(6). Money is DECIMAL(19,2), never float.
-- workspace_id and user IDs are plain values: they belong to identity-workspace, so there are no foreign keys to them.

--changeset finance:0001-1
CREATE TABLE categories (
    id           BINARY(16)  NOT NULL,
    workspace_id BINARY(16)  NOT NULL,
    type         VARCHAR(10) NOT NULL COMMENT 'INCOME or EXPENSE',
    name         VARCHAR(60) NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_workspace_type_name (workspace_id, type, name)
);
--rollback DROP TABLE categories;

--changeset finance:0001-2
CREATE TABLE transactions (
    id                 BINARY(16)    NOT NULL,
    workspace_id       BINARY(16)    NOT NULL,
    type               VARCHAR(10)   NOT NULL COMMENT 'INCOME or EXPENSE',
    description        VARCHAR(200)  NOT NULL,
    amount             DECIMAL(19,2) NOT NULL,
    currency           CHAR(3)       NOT NULL,
    category_id        BINARY(16)    NULL,
    payer_user_id      BINARY(16)    NOT NULL,
    occurred_at        DATETIME(6)   NOT NULL,
    created_by_user_id BINARY(16)    NOT NULL,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,
    deleted_at         DATETIME(6)   NULL COMMENT 'Soft delete',
    PRIMARY KEY (id),
    INDEX idx_transactions_workspace_occurred (workspace_id, deleted_at, occurred_at),
    INDEX idx_transactions_workspace_category (workspace_id, category_id, occurred_at),
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id)
);
--rollback DROP TABLE transactions;

--changeset finance:0001-3
CREATE TABLE transaction_splits (
    id             BINARY(16)    NOT NULL,
    transaction_id BINARY(16)    NOT NULL,
    user_id        BINARY(16)    NOT NULL,
    amount         DECIMAL(19,2) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_splits_transaction_user (transaction_id, user_id),
    CONSTRAINT fk_transaction_splits_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id)
);
--rollback DROP TABLE transaction_splits;

--changeset finance:0001-4
CREATE TABLE budgets (
    id           BINARY(16)    NOT NULL,
    workspace_id BINARY(16)    NOT NULL,
    category_id  BINARY(16)    NOT NULL,
    amount_limit DECIMAL(19,2) NOT NULL COMMENT 'Per calendar month (UTC)',
    currency     CHAR(3)       NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_budgets_workspace_category_currency (workspace_id, category_id, currency),
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id)
);
--rollback DROP TABLE budgets;

--changeset finance:0001-5
CREATE TABLE budget_alerts (
    budget_id         BINARY(16)  NOT NULL,
    period_start      DATE        NOT NULL,
    threshold_percent INT         NOT NULL,
    created_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (budget_id, period_start, threshold_percent),
    CONSTRAINT fk_budget_alerts_budget FOREIGN KEY (budget_id) REFERENCES budgets (id)
);
--rollback DROP TABLE budget_alerts;

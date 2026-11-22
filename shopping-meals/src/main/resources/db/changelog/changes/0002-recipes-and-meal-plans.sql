--liquibase formatted sql

-- Phase 3: recipes and the weekly meal plan. IDs are UUIDv7 in BINARY(16); plan dates are the household's
-- calendar days (DATE, no time zone).

--changeset shopping-meals:0002-1
CREATE TABLE recipes (
    id                 BINARY(16)    NOT NULL,
    workspace_id       BINARY(16)    NOT NULL,
    name               VARCHAR(120)  NOT NULL,
    servings           INT           NULL,
    instructions       TEXT          NULL,
    source_url         VARCHAR(500)  NULL,
    created_by_user_id BINARY(16)    NOT NULL,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_recipes_workspace (workspace_id, name)
);
--rollback DROP TABLE recipes;

--changeset shopping-meals:0002-2
CREATE TABLE recipe_ingredients (
    id        BINARY(16)   NOT NULL,
    recipe_id BINARY(16)   NOT NULL,
    position  INT          NOT NULL,
    name      VARCHAR(120) NOT NULL,
    quantity  VARCHAR(40)  NULL COMMENT 'Free text, like shopping items: 2, 500 g, a bunch',
    PRIMARY KEY (id),
    INDEX idx_recipe_ingredients_recipe (recipe_id, position),
    CONSTRAINT fk_recipe_ingredients_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE
);
--rollback DROP TABLE recipe_ingredients;

--changeset shopping-meals:0002-3
CREATE TABLE meal_plan_entries (
    id                 BINARY(16)   NOT NULL,
    workspace_id       BINARY(16)   NOT NULL,
    plan_date          DATE         NOT NULL,
    slot               VARCHAR(20)  NOT NULL COMMENT 'BREAKFAST, LUNCH, DINNER or SNACK',
    recipe_id          BINARY(16)   NULL COMMENT 'Cleared when the recipe is deleted; the title stays',
    title              VARCHAR(120) NOT NULL,
    note               VARCHAR(300) NULL,
    created_by_user_id BINARY(16)   NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_meal_plan_workspace_date (workspace_id, plan_date),
    CONSTRAINT fk_meal_plan_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE SET NULL
);
--rollback DROP TABLE meal_plan_entries;

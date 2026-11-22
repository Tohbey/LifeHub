--liquibase formatted sql

-- Names now come from identity-workspace (GET /internal/v1/users?userId=...), the single source of truth for
-- people. Drops the name table an earlier build of this service may have created.

--changeset audit:0002-drop-person-names
DROP TABLE IF EXISTS audit_person_names;
--rollback SELECT 1;

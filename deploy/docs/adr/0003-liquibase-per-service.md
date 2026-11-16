# 0003. Liquibase for schema management, per service

- Status: Accepted
- Date: 2026-11-15

## Context

Each service owns one MySQL schema. Schema changes must be versioned, applied exactly once per environment, and safe across rollbacks of the service jar. Flyway is not used. Hibernate schema generation has no history and is unsafe on real data.

## Decision

- Each service manages its own schema with Liquibase, run by Spring Boot at service startup.
- Layout inside each service:

  ```
  src/main/resources/db/changelog/
  ├── db.changelog-master.yaml      # includes the files below, in order
  └── changes/
      ├── 0001-initial-schema.sql
      └── 0002-....sql
  ```

- Changesets are written as Liquibase **formatted SQL** (`--liquibase formatted sql`, `--changeset lifehub:0001-1`). The master changelog is YAML and only includes files.
- Each changeset has a `--rollback` block where a rollback is possible.
- Liquibase runs with a **separate migration user** per schema (`<service>_migrator`, with DDL rights on that schema only), configured through `spring.liquibase.user` and `spring.liquibase.password`. The service's runtime user has DML rights only.
- Hibernate runs with `spring.jpa.hibernate.ddl-auto=validate` everywhere, so a mismatch between entities and schema fails at startup instead of at the first query.
- Never edit a changeset that has been applied anywhere. Add a new one.
- Changes follow expand and contract: add the new column or table, deploy code that uses it, remove the old one in a later release. The previous version of a service must keep working against the new schema.

## Consequences

- `DATABASECHANGELOG` and `DATABASECHANGELOGLOCK` live in each service schema. The lock table makes concurrent startups safe.
- Every service needs two sets of database credentials: `DB_USER`/`DB_PASSWORD` and `DB_MIGRATION_USER`/`DB_MIGRATION_PASSWORD`.
- Integration tests (Testcontainers) run the real changelog, so tests cover the schema the service actually ships with.
- Changesets are plain MySQL SQL, so they are not portable to another database. This is acceptable: MySQL is the only target.

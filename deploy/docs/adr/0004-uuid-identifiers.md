# 0004. UUIDv7 identifiers stored as BINARY(16)

- Status: Accepted
- Date: 2026-11-15

## Context

IDs cross service boundaries constantly: a `workspaceId` from identity-workspace is stored in every other schema, user IDs appear in finance splits, and every event carries IDs. IDs also appear in URLs (`/api/v1/workspaces/{workspaceId}/...`). Auto-increment numbers are guessable, unique only per table, and only known after the insert.

## Decision

- Every primary key and every cross-service reference is a UUID. This includes users, workspaces, business records and `eventId`.
- Use **UUID version 7** (time-ordered), so new rows are inserted at the end of the primary key index instead of at random positions.
- IDs are generated in application code before the insert, through one helper in `lifehub-starter`. The generator library is chosen and pinned in `lifehub-parent`.
- MySQL stores them as `BINARY(16)`. Use `BIN_TO_UUID(id)` and `UUID_TO_BIN('...')` for manual queries. Do not use the swap flag: UUIDv7 is already time-ordered.
- APIs and events represent them as the standard lowercase string form, for example `"0193a4b2-7c1e-7d3a-9f00-2b6c1e4d8a10"`.
- The Logto subject (`sub`) is not a LifeHub ID. `identity-workspace` maps it to a LifeHub user UUID on first login, and only that UUID is used inside LifeHub.

## Consequences

- IDs are not guessable, though membership checks remain the real protection.
- A record's ID is known before it is saved, so the same transaction can write the outbox event that references it.
- Keys are 16 bytes instead of 8. Acceptable at family scale.
- Raw `BINARY(16)` values are unreadable in a SQL client without `BIN_TO_UUID`.

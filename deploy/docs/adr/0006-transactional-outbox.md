# 0006. Transactional outbox and idempotent consumers

- Status: Accepted
- Date: 2026-11-15

## Context

A service must never commit a business change and lose its event, or publish an event for a change that was rolled back. Writing to MySQL and Kafka in one atomic step is not possible.

## Decision

- **Producers** write each event to an `outbox` table in the same database transaction as the business change. A publisher in `lifehub-starter` reads unsent rows in order, sends them to Kafka, and marks them sent once Kafka acknowledges.
- **Delivery is at least once.** A crash between send and mark-sent causes a duplicate.
- **Consumers** record each handled `eventId` in a `processed_events` table, in the same transaction as their own change, and skip events already recorded.
- Failed events are retried with backoff on `<topic>.retry`, then parked on `<topic>.dlt` for an operator.
- The envelope, outbox, publisher and consumer helpers are defined once in `lifehub-starter`. Event contents are catalogued in [`events.md`](../events.md).

## Consequences

- Business change and event are always consistent.
- Every consumer must be idempotent, and tests must prove it by delivering the same event twice.
- The outbox needs monitoring: old unsent rows mean the publisher or Kafka is stuck.
- Sent outbox rows and old `processed_events` rows are cleaned up on a schedule (retention to be set when the starter is built, for example 14 days).

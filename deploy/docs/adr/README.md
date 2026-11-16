# Architecture decision records

Short records of significant decisions: what was decided, why, and what it costs. One file per decision, numbered in order. Never rewrite an accepted ADR. If a decision changes, add a new ADR that supersedes the old one and update the old one's status.

| ADR | Title | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-microservices-in-a-monorepo.md) | Microservices in a Maven monorepo | Accepted |
| [0003](0003-liquibase-per-service.md) | Liquibase for schema management, per service | Accepted |
| [0004](0004-uuid-identifiers.md) | UUIDv7 identifiers stored as BINARY(16) | Accepted |
| [0005](0005-membership-via-identity-api.md) | Membership checks through the identity-workspace API | Accepted |
| [0006](0006-transactional-outbox.md) | Transactional outbox and idempotent consumers | Accepted |
| [0007](0007-two-token-types.md) | Separate user and service tokens | Accepted |
| [0008](0008-time-based-events-from-owning-service.md) | Time-based events are raised by the owning service | Proposed |
| [0009](0009-layered-package-structure.md) | Layered package structure inside each service | Accepted |
| [0010](0010-gateway-as-a-service.md) | The gateway is its own service; no service registry | Superseded by 0011 |
| [0011](0011-routing-in-app-with-eureka.md) | Routing in lifehub-app, services found through Eureka | Accepted |

## Template

```markdown
# NNNN. Title

- Status: Proposed | Accepted | Superseded by NNNN
- Date: YYYY-MM-DD

## Context
What problem or force makes a decision necessary.

## Decision
What we will do, stated plainly.

## Consequences
What gets easier, what gets harder, and what we must now do.
```

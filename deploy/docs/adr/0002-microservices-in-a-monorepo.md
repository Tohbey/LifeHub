# 0002. Microservices in a Maven monorepo

- Status: Accepted
- Date: 2026-11-15

## Context

LifeHub is a family app running on one homelab server. A modular monolith would be cheaper to run and easier to debug. The project is also a learning and showcase project, where practising service boundaries, events and independent deployment is part of the goal.

## Decision

- Build LifeHub as separate Spring Boot services: `identity-workspace`, `finance`, `planner`, `shopping-meals`, `document-inventory`, `notes`, `notification`, `audit`.
- Keep them in one repository with one Maven reactor (`lifehub-parent`) and one Jenkins job.
- Services depend only on `lifehub-starter`. Services never import each other, share repositories or join across schemas.
- `lifehub-starter` holds plumbing only (events, outbox, security, membership client, error handling, pagination). No business logic.
- `lifehub-app` is the only public entry point: Spring Cloud Gateway plus the built Angular PWA.

## Consequences

- Several JVMs plus Kafka on one host. Each service gets an explicit heap limit, starting at `-Xmx256m`.
- No cross-service transactions. Consistency between services is eventual, through events.
- More deployment, monitoring and debugging work than a monolith.
- The monorepo keeps refactors across the starter and services atomic, and keeps versions in one place.
- Building the foundation (parent, starter, gateway, app, identity-workspace) before other services is mandatory, so every service is built on the same plumbing.

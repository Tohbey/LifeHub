# 0009. Layered package structure inside each service

- Status: Accepted
- Date: 2026-11-15

## Context

The first version of `identity-workspace` used `api/`, `application/`, `domain/` and `infrastructure/` packages. The project owner prefers the widely used layered Spring Boot layout, which is easier to recognise and to follow consistently across eight services.

## Decision

Every business service uses these packages under `com.lifehub.<module>`: `config`, `controller`, `service` (interface plus `...Impl`), `repository`, `model/entity`, `model/dto`, `model/event`, `mapper`, `exception`, `util`. The README section "Inside each service" lists the rules.

`lifehub-starter` and `lifehub-gateway` are libraries, not applications, and keep packages named after what they provide (`events`, `membership`, `security`, `web`, `id`, `persistence`). `lifehub-app` uses the same layout as a service where it applies (`config/`).

## Consequences

- One structure to learn; a new service can copy `identity-workspace`.
- Grouping is by layer, not by feature. A service with many features will have long `controller/` and `service/` packages. At LifeHub's size per service, this is acceptable.
- Every service has an interface with a single implementation. This adds a file per service class in exchange for a clear contract that controllers depend on.

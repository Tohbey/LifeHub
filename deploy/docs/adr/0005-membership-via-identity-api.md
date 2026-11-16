# 0005. Membership checks through the identity-workspace API

- Status: Accepted
- Date: 2026-11-15

## Context

Every workspace-scoped request must confirm that the caller belongs to the workspace and has a high enough role. Roles differ per household, so they cannot live in Logto. Three options were considered:

1. Each service calls `identity-workspace` and caches the answer briefly.
2. Each service keeps a local copy of memberships, fed by Kafka events.
3. Memberships are added to the JWT through Logto token enrichment.

## Decision

Option 1. `lifehub-starter` provides a membership client that calls
`GET /internal/v1/workspaces/{workspaceId}/members?sub={logtoSubject}` (and a bulk variant taking several user IDs) with the service's machine-to-machine token.

- Answers are cached per workspace and user for 30 seconds by default (configurable).
- Timeouts and a circuit breaker protect the call. If `identity-workspace` cannot answer, the request is **denied** (fail closed).
- `/internal/**` is never routed by `lifehub-app` and accepts only service tokens.

## Consequences

- One source of truth for membership. No copies to drift, no membership events for services to consume.
- `identity-workspace` is on the path of every workspace request and must stay healthy and fast.
- A removed member can keep access for up to the cache time.
- Option 2 can be revisited if `identity-workspace` becomes a bottleneck. Option 3 was rejected because tokens would go stale until refresh and grow with the number of households.

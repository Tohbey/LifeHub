# 0007. Separate user and service tokens

- Status: Accepted
- Date: 2026-11-15

## Context

Two kinds of caller reach LifeHub services: a signed-in person through the PWA, and another LifeHub service (for example a membership lookup). Reusing the user's token for service calls would let any service act as the user anywhere, and would fail for event consumers, which have no user token.

## Decision

- **User tokens** (human-to-machine): issued by Logto to the Angular PWA through Authorization Code with PKCE. The PWA is a public client with no secret. Audience: the public API (`LOGTO_AUDIENCE`). Accepted only on `/api/v1/**`.
- **Service tokens** (machine-to-machine): issued by Logto through the client credentials grant. One Logto application per service, each with only the scopes it needs. Audience: the internal API (`LOGTO_INTERNAL_AUDIENCE`). Accepted only on `/internal/**`.
- `lifehub-starter` fetches service tokens, caches them until shortly before expiry, and attaches them to internal calls.
- A service token identifies the calling service, not a user. When a user matters, the user is passed explicitly, taken from the already validated user token.
- Both the gateway and each service validate issuer, signature, expiry and audience.

## Consequences

- A leaked service secret exposes only that service's internal scopes, not user data through the public API.
- Each service needs its own `LOGTO_M2M_CLIENT_ID` and `LOGTO_M2M_CLIENT_SECRET`.
- Event consumers rely on `actorUserId` in the event envelope, and trust events only because they come from Kafka, which is reachable only on the Docker network.

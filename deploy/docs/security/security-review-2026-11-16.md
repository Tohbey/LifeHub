# Security review 2026-11-16

- Scope: every module's `src/main` Java code, POMs, Liquibase changelogs, Dockerfiles and `deploy/env` templates. `application.yaml` files were excluded at the owner's request.
- Commit / state: uncommitted working tree
- Summary: 1 High, 3 Medium, 3 Low, 3 Info. The token checks, the internal API and workspace isolation hold up. The real problems were race conditions: two people could both join a household through one invite link, and two ownership handovers at the same moment could leave two owners. Both are fixed. The open items need an owner decision: the registry has no login, there is no rate limiting, and email addresses are not verified.

## Findings

| ID | Severity | Title | Location | Status |
|---|---|---|---|---|
| SEC-001 | High | An invite link can be used twice by concurrent requests | `identity-workspace/.../InvitationServiceImpl.java:45` | Fixed |
| SEC-002 | Medium | Concurrent membership changes can leave a household with two owners | `identity-workspace/.../HouseholdServiceImpl.java` | Fixed |
| SEC-003 | Medium | Unauthenticated Eureka registry lets anything on the network take over a service name | `lifehub-service-registry` | Open |
| SEC-004 | Medium | No rate limiting at the gateway | `lifehub-app` | Open |
| SEC-005 | Low | Tokens without a `sub` claim are accepted | `LogtoJwtDecoders.java`, `SecurityConfig.java` | Fixed |
| SEC-006 | Low | Unverified profile email decides who is told about an invitation | `HouseholdServiceImpl.java:162`, `User.java` | Open |
| SEC-007 | Low | Invite token travels in the URL path | `InvitationController.java:23` | Open |
| SEC-008 | Info | Unknown `sort` property returns 500 | `ApiExceptionHandler.java` | Fixed |
| SEC-009 | Info | API docs are public through lifehub-app | `lifehub-app` | Won't fix (requested feature, can be turned off) |
| SEC-010 | Info | Docker base images are not pinned by digest | `*/Dockerfile` | Open |

### SEC-001: An invite link can be used twice by concurrent requests
- Severity: High
- Location: `identity-workspace/src/main/java/com/lifehub/identityworkspace/service/InvitationServiceImpl.java:45`
- Problem: the invitation was read with a plain SELECT, checked for `PENDING`, then marked `ACCEPTED`. Two transactions could both read `PENDING` before either one committed.
- Impact: a forwarded or leaked link, used by two accounts at the same moment, adds both people to the household. That breaks the single-use rule ADR 0005 and the README rely on.
- Fix: `HouseholdInvitationRepository.findByTokenHash` now takes a `PESSIMISTIC_WRITE` lock (`SELECT ... FOR UPDATE`). A second request waits, then sees `ACCEPTED` and gets 404. New test `IdentityWorkspaceIntegrationTest.anInviteLinkUsedByTwoPeopleAtOnceAddsOnlyOne`, which needs Docker.
- Status: Fixed

### SEC-002: Concurrent membership changes can leave a household with two owners
- Severity: Medium
- Location: `identity-workspace/src/main/java/com/lifehub/identityworkspace/service/HouseholdServiceImpl.java`, in `changeMemberRole`, `removeMember` and `inviteMember`
- Problem: the actor's role was checked against an unlocked read. Two ownership handovers sent at once by the owner (to different members) both passed the "actor is OWNER" check. Each one demoted the owner and promoted its target.
- Impact: two owners, which breaks the "exactly one owner" rule. A similar race existed between a role change and a removal.
- Fix: these three use cases now start with `requireMemberForUpdate`, which locks the household row (`HouseholdRepository.findByIdForUpdate`, `PESSIMISTIC_WRITE`). Membership changes within one household now run one at a time, and each sees the previous one's result.
- Status: Fixed

### SEC-003: Unauthenticated Eureka registry lets anything on the network take over a service name
- Severity: Medium
- Location: `lifehub-service-registry` (no security on `/eureka/**`)
- Problem: any process that can reach port 8761 can register an instance called `identity-workspace` (or any other name).
- Impact: lifehub-app would then send some user requests, including their `Authorization: Bearer` tokens, to that instance. ADR 0011 relies on the registry being reachable only from the Docker network.
- Fix (recommended): keep 8761 unpublished in Compose (planned). Also add HTTP Basic auth to the registry, using credentials in `EUREKA_URL` (`http://user:pass@lifehub-service-registry:8761/eureka`).
- Status: Open. This changes the setup recorded in ADR 0011, so it needs the owner's decision.

### SEC-004: No rate limiting at the gateway
- Severity: Medium
- Location: `lifehub-app`
- Problem: nothing limits request rates, either per user or per IP.
- Impact: a buggy client or an attacker can flood the services and MySQL. Invite tokens are 256-bit, so guessing them is not a realistic risk.
- Fix (recommended): Spring Cloud Gateway's `RequestRateLimiter`, or limits in Nginx Proxy Manager. The Gateway option needs Redis, which is a new infrastructure piece.
- Status: Open. This is already on the stage 1 gap list.

### SEC-005: Tokens without a `sub` claim are accepted
- Severity: Low
- Location: `lifehub-starter/.../security/LogtoJwtDecoders.java`, `lifehub-app/.../config/SecurityConfig.java`
- Problem: the decoders checked signature, expiry, issuer and audience, but not that `sub` is present. Every use case keys the caller on `sub`.
- Impact: a validly signed token with no `sub` would reach provisioning with a null subject. That would cause 500 errors and, depending on the code path, could mix up callers.
- Fix: a `JwtClaimValidator` now requires a non-blank `sub` in both decoders.
- Status: Fixed

### SEC-006: Unverified profile email decides who is told about an invitation
- Severity: Low
- Location: `HouseholdServiceImpl.inviteMember` (`users.findFirstByEmail`), `User.updateProfile`
- Problem: anyone can set any email on their profile through `PATCH /api/v1/me`. When someone is invited by email, the first user with that email is put in the `member.invited` event as `invitedUserId`.
- Impact: once notifications exist, a user who sets a victim's email address would be shown invitations meant for the victim: household name, role and inviter. The invite token is not in the event, so they cannot join.
- Fix (recommended): take the email from Logto's verified `email` claim (ID token or userinfo) instead of free text. Alternatively, only link users whose email is marked verified.
- Status: Open. This changes how the profile works, so it needs the owner's decision.

### SEC-007: Invite token travels in the URL path
- Severity: Low
- Location: `InvitationController` (`POST /api/v1/invitations/{token}/accept`)
- Problem: anything in a URL path tends to end up in access logs (Nginx Proxy Manager, gateway).
- Impact: anyone who can read those logs can use an invite token while it is still pending.
- Fix (recommended): send the token in the request body (`POST /api/v1/invitations/accept`), and keep access logs to path patterns only.
- Status: Open. This changes the API contract.

### SEC-008: Unknown `sort` property returns 500
- Severity: Info
- Location: `lifehub-starter/.../web/ApiExceptionHandler.java`
- Problem: `?sort=doesNotExist` on a paged endpoint threw `PropertyReferenceException`, which became a 500.
- Impact: misleading errors and noisy logs. Entity property names cannot be used to reach other data.
- Fix: the handler now maps it to 400 "Unknown sort property."
- Status: Fixed

### SEC-009: API docs are public through lifehub-app
- Severity: Info
- Problem: `/swagger-ui.html` and `/openapi/<service>` on lifehub-app need no login. Only the public API group is exposed; `/internal/**` docs are never routed, and every API call still needs a token.
- Status: Won't fix. Swagger was requested. Set `SPRINGDOC_API_DOCS_ENABLED=false` and `SPRINGDOC_SWAGGER_UI_ENABLED=false` in production if the API shape should not be visible.

### SEC-010: Docker base images are not pinned by digest
- Severity: Info
- Location: `*/Dockerfile` (`eclipse-temurin:21-jre`)
- Problem: the tag is not pinned, so a rebuild can pull a different image.
- Fix (recommended): pin to `eclipse-temurin:21-jre@sha256:...` in the Jenkins pipeline and update the digest on purpose.
- Status: Open

## Checked and found no issue

- **JWT validation:** signature with an explicit ES384/RS256 allow-list, expiry, issuer and audience, separately for user tokens (`/api/**`) and service tokens (`/internal/**`).
- **Internal API:** `/internal/**` is denied in lifehub-app and has no route. The internal membership API requires `SCOPE_membership:read`, which the new `SecurityChainsTest` confirms without Docker. Bulk lookups are capped at 100.
- **Caller identity:** always taken from the token's `sub`, never from the request body, header or path.
- **Workspace isolation:** a non-member gets 404, the same as for a household that does not exist; a personal workspace is not reachable through the household endpoints; a role that is too low gets 403.
- **Membership checks:** an unavailable lookup becomes 503 (fail closed), and only positive answers are cached.
- **Role rules (`MemberPolicy`):** a member cannot raise their own role, the owner cannot leave, and an invitation can never make someone owner.
- **SQL:** JPQL and `JdbcClient` use bound parameters only. The Liquibase changesets are plain DDL.
- **Request validation:** Bean Validation on every request DTO, with size limits.
- **Invite tokens:** 256 bits from `SecureRandom`, only the SHA-256 hash is stored (with a unique key), and they expire after 7 days.
- **Error responses:** they carry no stack traces or SQL. `ApiException` details are written for users.
- **Events:** payloads contain no tokens. The outbox and processed-events SQL is parameterised.
- **Dockerfiles:** containers run as non-root (uid 10001).
- **Secrets:** none in the Java sources, POMs or env templates (the templates use `change-me` placeholders), and `.gitignore` excludes real `*.env` files.

## Not reviewed

- `application.yaml` files (excluded). While editing for Swagger, I saw that `identity-workspace/src/main/resources/application.yaml` contains a hard-coded MySQL `root` password and LAN address. That file needs the planned review before anything is committed.

## Verification

- Command: `./mvnw clean install` from the repository root.
- Result: BUILD SUCCESS. 86 tests run, 0 failures, 0 errors, 7 skipped. The 7 skipped tests are `IdentityWorkspaceIntegrationTest`, which needs Docker; Docker was not running, so they did not run.

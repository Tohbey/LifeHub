# lifehub-web

The LifeHub PWA (Angular 21, standalone components, signals). It talks only to `lifehub-app`, which routes
`/api/**` to the services. In development the Angular dev server proxies `/api` to `lifehub-app` on port 8089
(`proxy.conf.json`), so the browser sees one origin, exactly as in production.

## One-time setup

### 1. Create the Logto application

In the Logto console, **Applications → Create application → Single page app** (any SPA framework), named
for example "LifeHub Web". In its settings:

| Setting | Value |
|---|---|
| Redirect URIs | `http://localhost:4200/callback` (and later `https://<your LifeHub host>/callback`) |
| Post sign-out redirect URIs | `http://localhost:4200` (and later `https://<your LifeHub host>`) |
| CORS allowed origins | `http://localhost:4200` (and later your LifeHub host), if Logto does not add it from the redirect URIs |

Users sign in to this app; the access token it gets is for the `LifeHub API` resource, the same one the
services check (`lifehub.security.audience`). Create two or more users in Logto (**Users**) to test households.

### 2. Point the app at Logto

Edit `public/app-config.json`:

```json
{
  "logtoEndpoint": "https://auth.homelab.dev",
  "logtoAppId": "<the app ID of the SPA application>",
  "apiResource": "https://api.lifehub.homelab.dev"
}
```

`apiResource` must equal `lifehub.security.audience`. The file is read at startup, so changing it needs no rebuild.

### 3. Trust the homelab certificate in the browser

Logto runs on `https://auth.homelab.dev` with an mkcert certificate. The browser must trust that CA, or sign-in
fails before it starts: `sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain homelab-ca.pem`.

## Run

```
npm install          # .npmrc sets legacy-peer-deps, which npm 10.2 needs here
npm start            # http://localhost:4200, with /api proxied to lifehub-app on 8089
```

Start the registry, the services and `lifehub-app` first. To act as two people at once, use a second browser
profile or a private window and sign in as the other user.

## What each page calls

| Page | Service | Endpoints |
|---|---|---|
| Home | finance, planner, shopping-meals, notification | summary, open tasks, next 7 days, lists, unread count: each card shows its own error |
| Household | identity-workspace | households, members, roles, invitations, accept |
| Finance | finance | transactions (with splits), categories, budgets, summary |
| Tasks, Calendar | planner | tasks, events, attendees, reminders |
| Shopping | shopping-meals | lists, items |
| Notifications, bell | notification | list, read state, live stream (server-sent events) |
| Audit log | audit | the workspace's events (owners and admins) |
| Profile | identity-workspace, notification | profile, notification settings |

Every failed call shows a message with the HTTP status and the API's `detail`, plus the invalid fields for
validation errors.

## Build

`npm run build` writes `dist/lifehub-web/browser`. The production build includes the service worker
(`ngsw-config.json`), which caches the app's own files only, never API responses or tokens (README section 12).

### Built into lifehub-app

This folder is also a Maven module. `./mvnw package` (from the repository root) downloads Node 22 into
`target/` (cached in `~/.m2`), runs `npm ci` and `npm run build`, and packages the output under `static/` in
`lifehub-web.jar`. `lifehub-app` depends on that jar, so its own jar serves the UI at `/`, with deep links such as
`/finance` falling back to `index.html`. No Node installation is needed to build the jar.

| Command | What you get |
|---|---|
| `./mvnw -pl lifehub-app -am package` | lifehub-app with the UI built in |
| `./mvnw -pl lifehub-app -am package -Dlifehub.web.skip=true` | Backend only, no Node step: lifehub-app serves no UI |
| `npm start` in this folder | The standalone dev server on 4200, with live reload |

`public/app-config.json` is packaged too. The redirect URIs are built from the page's own address, so the same file
works at `http://localhost:8089` as long as the Logto SPA app lists `http://localhost:8089/callback` (and the
production host) as redirect URIs.

## Functional tests (Playwright)

```
npx playwright install chromium   # once
npm run e2e                       # headless; starts its own dev server on port 4300
npm run e2e:ui                    # Playwright's UI, to watch and step through tests
```

The tests drive the real app in Chromium against a **mocked API**: no backend, Logto or database is needed.

- `e2e/support/mock-backend.ts` answers every `/api/**` call in the browser, with small stateful defaults
  (creating a task makes it appear in the list) and records each request, so a test can check the exact method,
  path, body and `Authorization` header the UI sent. A spec overrides any endpoint with `backend.on(...)`.
- `e2e/support/fixtures.ts` starts each test signed in by putting a Logto session (ID token and an access token
  for the API resource) where the Logto SDK keeps it, and serves a test `app-config.json`. Nothing in the app
  is test-only. `test.use({ signedIn: false })` starts signed out.
- `layout.spec.ts` also runs on a phone-sized screen (the `mobile` project), for the bottom navigation.

| Spec | Covers |
|---|---|
| `auth` | Redirect to Logto with PKCE and the API resource; bearer token on every API call; account load error and retry |
| `layout` | User, unread badge, workspace switcher (remembered across reloads), navigation on desktop and mobile |
| `dashboard` | Each card loads independently; a failing service only breaks its own card |
| `finance` | Shared expense with an equal split (rounding), field-by-field validation errors, delete, admin-only budgets |
| `household` | Create and switch, invite link, accepting an invite, a used invite link |
| `planner` | Create, assign and complete a task; an event with attendees and a reminder |
| `shopping` | Add an item and tick it off |
| `notifications` | Live stream toast and badge, open a notification in its workspace, mark all read |
| `audit` | Admins see events and payloads; members do not get the page |

A failed test leaves a screenshot and a trace in `test-results/` (`npx playwright show-trace <trace.zip>`), and an
HTML report in `playwright-report/`.

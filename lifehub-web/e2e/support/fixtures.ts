import { test as base, expect } from '@playwright/test';

import { ALICE } from './data';
import { MockBackend } from './mock-backend';

export const APP_ID = 'e2e-app';
export const API_RESOURCE = 'https://api.lifehub.test';
export const ACCESS_TOKEN = 'e2e-access-token';
export const LOGTO = 'https://logto.lifehub.test';

/** A base64url JSON segment of a JWT. */
function segment(value: object): string {
  return Buffer.from(JSON.stringify(value)).toString('base64url');
}

/**
 * A signed-in Logto session as the SDK stores it: an ID token (decoded, never verified, by the SDK) and an
 * access token for the API resource, so the SDK never needs to call Logto. Valid for 30 days, so specs that fix
 * the browser's clock (page.clock) at a later time of day still have a live session.
 */
export function logtoSession(sub = 'logto-alice') {
  const now = Math.floor(Date.now() / 1000);
  const exp = now + 30 * 86400;
  const idToken = [segment({ alg: 'ES384', typ: 'JWT' }),
    segment({ iss: `${LOGTO}/oidc`, sub, aud: APP_ID, exp, iat: now, name: ALICE.displayName, email: ALICE.email }),
    'signature'].join('.');
  const accessTokens = { [`@${API_RESOURCE}`]: { token: ACCESS_TOKEN, scope: '', expiresAt: exp } };
  return { [`logto:${APP_ID}:idToken`]: idToken, [`logto:${APP_ID}:accessToken`]: JSON.stringify(accessTokens) };
}

interface Fixtures {
  /** The fake API; change its data or handlers before navigating. */
  backend: MockBackend;
  /** Set to false in a spec (test.use) to start signed out. */
  signedIn: boolean;
}

/**
 * Every test gets a fresh {@link MockBackend}, the test app-config.json, and (unless signedIn is false) a Logto
 * session in localStorage. Any request to the real network that is not localhost fails the test.
 */
export const test = base.extend<Fixtures>({
  signedIn: [true, { option: true }],
  // auto: every test gets the mocks and the session, even one that never touches `backend` itself.
  backend: [async ({ page, signedIn }, use) => {
    const backend = new MockBackend();
    await page.route('**/app-config.json', (route) =>
      route.fulfill({ json: { logtoEndpoint: LOGTO, logtoAppId: APP_ID, apiResource: API_RESOURCE } }));
    await backend.install(page);
    if (signedIn) {
      await page.addInitScript((items) => {
        for (const [key, value] of Object.entries(items)) {
          localStorage.setItem(key, value);
        }
      }, logtoSession());
    }
    await use(backend);
  }, { auto: true }],
});

export { expect };

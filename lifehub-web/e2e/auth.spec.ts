import { ACCESS_TOKEN, API_RESOURCE, APP_ID, LOGTO, expect, test } from './support/fixtures';

test.describe('signed out', () => {
  test.use({ signedIn: false });

  test('sends the user to Logto with PKCE for the LifeHub API', async ({ page, backend }) => {
    await page.route(`${LOGTO}/oidc/.well-known/openid-configuration`, (route) => route.fulfill({
      json: {
        issuer: `${LOGTO}/oidc`, authorization_endpoint: `${LOGTO}/oidc/auth`, token_endpoint: `${LOGTO}/oidc/token`,
        end_session_endpoint: `${LOGTO}/oidc/session/end`, revocation_endpoint: `${LOGTO}/oidc/token/revocation`,
        userinfo_endpoint: `${LOGTO}/oidc/me`, jwks_uri: `${LOGTO}/oidc/jwks`,
      },
    }));
    await page.route(`${LOGTO}/oidc/auth**`, (route) => route.fulfill({ contentType: 'text/html', body: '<h1>Logto sign-in</h1>' }));

    await page.goto('/finance');

    await expect(page.getByRole('heading', { name: 'Logto sign-in' })).toBeVisible();
    const url = new URL(page.url());
    expect(url.origin + url.pathname).toBe(`${LOGTO}/oidc/auth`);
    expect(url.searchParams.get('client_id')).toBe(APP_ID);
    expect(url.searchParams.get('redirect_uri')).toBe('http://localhost:4300/callback');
    expect(url.searchParams.get('code_challenge_method')).toBe('S256');
    expect(url.searchParams.get('code_challenge')).toBeTruthy();
    expect(url.searchParams.getAll('resource')).toContain(API_RESOURCE);
    expect(backend.requests).toHaveLength(0);
  });
});

test('every API call carries the access token, and nothing else does', async ({ page, backend }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1, name: /, Alice$/ })).toBeVisible();

  expect(backend.requests.length).toBeGreaterThan(3);
  for (const request of backend.requests) {
    expect(request.authorization, `${request.method} ${request.path}`).toBe(`Bearer ${ACCESS_TOKEN}`);
  }
});

test('shows why the account could not load, and retries', async ({ page, backend }) => {
  backend.on('GET', /^\/api\/v1\/me$/, () => ({ status: 503, body: { title: 'Service Unavailable', status: 503 } }));
  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'Could not load your account' })).toBeVisible();
  await expect(page.getByText('503 Service Unavailable')).toBeVisible();

  backend.on('GET', /^\/api\/v1\/me$/, () => ({ body: backend.me }));
  await page.getByRole('button', { name: 'Try again' }).click();
  await expect(page.getByRole('heading', { level: 1, name: /, Alice$/ })).toBeVisible();
});

test('a profile without a name gets the name from Logto on sign-in', async ({ page, backend }) => {
  backend.me = { ...backend.me, displayName: null as unknown as string };
  await page.goto('/');

  await expect(page.getByRole('heading', { level: 1, name: /, Alice$/ })).toBeVisible();
  expect(backend.calls('PATCH', /^\/api\/v1\/me$/).map((r) => r.body)).toEqual([{ displayName: 'Alice' }]);
});

test('a profile that has a name is left alone', async ({ page, backend }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1, name: /, Alice$/ })).toBeVisible();
  expect(backend.calls('PATCH', /^\/api\/v1\/me$/)).toHaveLength(0);
});

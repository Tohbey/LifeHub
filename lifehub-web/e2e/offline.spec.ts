import { expect, test } from '@playwright/test';

import { logtoSession } from './support/fixtures';

/**
 * Against the production build, with its service worker: once LifeHub has been opened, it still starts without a
 * network, because the app and its app-config.json come from the service worker's cache. With no backend the
 * shell shows its "could not load" card, which is enough to prove the app itself started.
 */
test('starts offline once it has been opened', async ({ page, context }) => {
  // Never reach a real Logto or API host. context.route also sees the service worker's own requests.
  await context.route(/^https:\/\/(?!localhost)/, (route) => route.abort());
  await page.addInitScript((items) => {
    for (const [key, value] of Object.entries(items)) {
      localStorage.setItem(key, value);
    }
  }, logtoSession());

  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Could not load your account' })).toBeVisible();

  // Wait until the service worker controls the page and has cached app-config.json.
  await page.evaluate(() => navigator.serviceWorker.ready);
  await page.reload();
  await expect.poll(() => page.evaluate(() => !!navigator.serviceWorker.controller), { timeout: 30_000 }).toBe(true);
  await expect.poll(() => page.evaluate(async () => (await caches.keys()).some((k) => k.includes('app-config'))),
    { timeout: 30_000 }).toBe(true);

  await context.setOffline(true);
  await page.reload();

  await expect(page.getByRole('heading', { name: 'Could not load your account' })).toBeVisible();
  await expect(page.getByText('You are offline. LifeHub will load your data again once you reconnect.')).toBeVisible();
  await context.setOffline(false);
});

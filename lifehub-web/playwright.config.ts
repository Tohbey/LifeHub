import { defineConfig, devices } from '@playwright/test';

/**
 * Functional tests of the UI against a mocked LifeHub API (e2e/support/mock-backend.ts). No backend, Logto or
 * database is needed: every /api call and the Logto session are faked in the browser.
 */
export default defineConfig({
  testDir: 'e2e',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: 'http://localhost:4300',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] }, testIgnore: /offline\.spec\.ts/ },
    // The service worker only runs in a production build, so this project tests the built app, served as-is.
    { name: 'offline', use: { ...devices['Desktop Chrome'], baseURL: 'http://localhost:4301' }, testMatch: /offline\.spec\.ts/ },
    { name: 'mobile', use: { ...devices['Pixel 7'] }, testMatch: /layout\.spec\.ts/ },
  ],
  // Its own port, so it never clashes with a dev server on 4200.
  webServer: [
    {
      command: 'npx ng serve --port 4300',
      url: 'http://localhost:4300',
      reuseExistingServer: !process.env['CI'],
      timeout: 180_000,
    },
    {
      command: 'npx ng build --output-path=dist/e2e && node e2e/support/serve-dist.mjs dist/e2e/browser 4301',
      url: 'http://localhost:4301',
      reuseExistingServer: !process.env['CI'],
      timeout: 180_000,
    },
  ],
});

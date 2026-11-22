import { expect, test } from './support/fixtures';

test('an unverified email can be sent a verification link', async ({ page, backend }) => {
  await page.goto('/profile');

  await expect(page.getByText('Not verified')).toBeVisible();
  await page.getByRole('button', { name: 'Send verification link' }).click();

  await expect(page.getByText('Link sent to alice@example.com.')).toBeVisible();
  expect(backend.calls('POST', /^\/api\/v1\/me\/email-verification$/)).toHaveLength(1);
});

test('opening the link confirms the email', async ({ page, backend }) => {
  await page.goto('/verify-email/tok-123');

  await expect(page.getByRole('heading', { name: 'Email confirmed' })).toBeVisible();
  expect(backend.calls('POST', /\/email-verification\/tok-123$/)).toHaveLength(1);
  await page.getByRole('link', { name: 'Back to your profile' }).click();
  await expect(page.getByText('Verified', { exact: true })).toBeVisible();
});

test('a used or foreign link explains itself', async ({ page, backend }) => {
  backend.on('POST', /\/email-verification\/old$/, () => ({
    status: 404, body: { title: 'Not Found', status: 404, detail: 'This link is invalid, used or expired.' },
  }));
  await page.goto('/verify-email/old');

  await expect(page.getByRole('heading', { name: 'That link did not work' })).toBeVisible();
  await expect(page.getByText('404 Not Found – This link is invalid, used or expired.')).toBeVisible();
});

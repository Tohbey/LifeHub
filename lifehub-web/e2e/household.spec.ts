import { HOME, PERSONAL } from './support/data';
import { expect, test } from './support/fixtures';

test('creating a household switches to it', async ({ page, backend }) => {
  const created = { id: 'house-new', name: 'Beach House', defaultCurrency: 'EUR', timezone: 'Europe/Lisbon',
    createdAt: '2026-11-16T09:00:00Z', yourRole: 'OWNER' };
  backend.workspaces = [PERSONAL];
  backend.on('POST', /^\/api\/v1\/households$/, () => {
    backend.workspaces = [PERSONAL, { id: created.id, type: 'HOUSEHOLD', name: created.name, role: 'OWNER' }];
    return { status: 201, body: created };
  });
  await page.goto('/household');

  const form = page.locator('section', { hasText: 'Create a household' });
  await form.getByLabel('Name').fill('Beach House');
  await form.getByLabel('Currency').selectOption('EUR');
  await form.getByLabel('Timezone').selectOption('Europe/Lisbon');
  await expect(form.getByLabel('Timezone').locator('option').first()).toHaveText('(UTC+00:00) Europe/London');
  await form.getByRole('button', { name: 'Create' }).click();

  await expect(page.getByText('Household "Beach House" created.')).toBeVisible();
  expect(backend.calls('POST', /^\/api\/v1\/households$/)[0].body)
    .toEqual({ name: 'Beach House', defaultCurrency: 'EUR', timezone: 'Europe/Lisbon' });
  await expect(page.getByRole('combobox', { name: 'Workspace' })).toHaveValue('house-new');
});

test('an invitation shows its one-time link', async ({ page, backend }) => {
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
  backend.on('POST', new RegExp(`/households/${HOME.id}/invitations$`), (r) => ({
    status: 201, body: { id: 'inv-1', email: r.body.email, role: r.body.role, expiresAt: '2026-11-23T09:00:00Z', token: 'tok-abc' },
  }));
  await page.goto('/household');

  await expect(page.getByRole('cell', { name: /Bob/ })).toBeVisible();
  await page.getByLabel('Email').fill('sam@example.com');
  await page.getByRole('button', { name: 'Create invite link' }).click();

  await expect(page.getByText('http://localhost:4300/invitations/tok-abc')).toBeVisible();
  expect(backend.calls('POST', /\/invitations$/)[0].body).toEqual({ email: 'sam@example.com', role: 'MEMBER' });
});

test('opening an invite link and accepting joins the household', async ({ page, backend }) => {
  backend.workspaces = [PERSONAL];
  backend.on('POST', /^\/api\/v1\/invitations\/tok-abc\/accept$/, () => {
    backend.workspaces = [PERSONAL, { ...HOME, role: 'MEMBER' }];
    return { body: { ...HOME, role: 'MEMBER' } };
  });
  await page.goto('/invitations/tok-abc');

  await page.getByRole('button', { name: 'Accept invitation' }).click();

  await expect(page.getByText('You joined Fafowora Home.')).toBeVisible();
  await expect(page).toHaveURL(/\/household$/);
  await expect(page.getByRole('combobox', { name: 'Workspace' })).toHaveValue(HOME.id);
});

test('a used invite link shows the API reason', async ({ page, backend }) => {
  backend.on('POST', /\/invitations\/tok-used\/accept$/, () => ({
    status: 404, body: { title: 'Not Found', status: 404, detail: 'This invitation is invalid, used or expired.' },
  }));
  await page.goto('/invitations/tok-used');
  await page.getByRole('button', { name: 'Accept invitation' }).click();

  await expect(page.getByText('404 Not Found – This invitation is invalid, used or expired.')).toBeVisible();
});

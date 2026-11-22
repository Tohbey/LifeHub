import { BOB, HOME, problem } from './support/data';
import { expect, test } from './support/fixtures';

test.beforeEach(async ({ page }) => {
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
});

test('records a shared expense split equally, with the exact body the API expects', async ({ page, backend }) => {
  await page.goto('/finance');

  await page.getByLabel('Description').fill('Electricity');
  await page.getByLabel('Amount').fill('120.01');
  // The new-entry form's Category comes before the list's "Filter by category".
  await page.getByRole('combobox', { name: 'Category' }).first().selectOption({ label: 'Food' });
  await page.getByLabel('Shared expense').check();
  await page.getByRole('button', { name: 'Record' }).click();

  await expect(page.getByText('Shared expense recorded; the others are notified.')).toBeVisible();
  const [post] = backend.calls('POST', new RegExp(`/workspaces/${HOME.id}/transactions$`));
  expect(post.body).toMatchObject({
    type: 'EXPENSE', description: 'Electricity', amount: 120.01, currency: 'GBP', categoryId: 'cat-food',
    payerUserId: backend.me.id,
  });
  // 120.01 split two ways: the extra cent goes to the first person, and the shares add up exactly.
  expect(post.body.splits).toEqual([{ userId: backend.me.id, amount: 60.01 }, { userId: BOB.id, amount: 60 }]);
  expect(new Date(post.body.occurredAt).toISOString()).toBe(post.body.occurredAt);
  expect(backend.calls('GET', /^\/api\/v1\/reference-data$/)).not.toHaveLength(0);
  await expect(page.getByRole('cell', { name: /Electricity/ })).toContainText('Split: You');
});

test('the currency is a dropdown from lifehub-app, defaulting to the household currency', async ({ page, backend }) => {
  backend.on('GET', /^\/api\/v1\/households\/[^/]+$/, (_r, m) => ({
    body: { id: HOME.id, name: HOME.name, defaultCurrency: 'NGN', timezone: 'Africa/Lagos', createdAt: '2026-11-01T09:00:00Z', yourRole: 'OWNER' },
  }));
  await page.goto('/finance');

  const currency = page.getByLabel('Currency');
  await expect(currency).toHaveValue('NGN');
  await expect(currency.locator('option')).toHaveText(['GBP · British Pound (£)', 'EUR · Euro (€)', 'NGN · Nigerian Naira', 'JPY · Japanese Yen (JP¥)']);

  await page.getByLabel('Description').fill('Jollof');
  await page.getByLabel('Amount').fill('4500');
  await currency.selectOption('EUR');
  await page.getByRole('button', { name: 'Record' }).click();
  await expect(page.getByText('Transaction recorded.')).toBeVisible();
  expect(backend.calls('POST', /\/transactions$/)[0].body.currency).toBe('EUR');
});

test('shows the API validation errors field by field', async ({ page, backend }) => {
  backend.on('POST', /\/transactions$/, () => ({
    status: 400, body: problem(400, 'Bad Request', 'Some fields are invalid.', { amount: 'must be greater than 0' }),
  }));
  await page.goto('/finance');

  await page.getByLabel('Description').fill('Refund');
  await page.getByLabel('Amount').fill('5');
  await page.getByRole('button', { name: 'Record' }).click();

  await expect(page.getByText('400 Bad Request – Some fields are invalid. – amount: must be greater than 0')).toBeVisible();
});

test('deletes a transaction after confirming', async ({ page, backend }) => {
  backend.transactions = [{ id: 'tx-1', type: 'EXPENSE', description: 'Lunch', amount: 9.5, currency: 'GBP', categoryId: null,
    payerUserId: backend.me.id, occurredAt: '2026-11-10T12:00:00Z', createdByUserId: backend.me.id, splits: [] }];
  backend.on('DELETE', /\/transactions\/tx-1$/, () => ({ status: 204 }));
  await page.goto('/finance');

  page.once('dialog', (dialog) => dialog.accept());
  await page.getByRole('row', { name: /Lunch/ }).getByRole('button', { name: 'Delete' }).click();

  await expect(page.getByText('Transaction deleted.')).toBeVisible();
  expect(backend.calls('DELETE', /\/transactions\/tx-1$/)).toHaveLength(1);
});

test('budgets are for admins only', async ({ page, backend }) => {
  backend.workspaces = [{ ...HOME, role: 'MEMBER' }];
  await page.goto('/finance');
  await page.getByRole('tab', { name: 'Budgets' }).click();

  await expect(page.getByText('Only owners and admins can set budgets.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Set budget' })).toHaveCount(0);
});

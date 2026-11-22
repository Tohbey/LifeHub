import { HOME } from './support/data';
import { expect, test } from './support/fixtures';

test.beforeEach(async ({ page }) => {
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
});

test('exports this month as a CSV download', async ({ page, backend }) => {
  await page.goto('/finance');

  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Export CSV (this month)' }).click();

  expect((await download).suggestedFilename()).toBe('lifehub-transactions-2026-11-01-to-2026-11-30.csv');
  expect(backend.calls('GET', /\/finance\/export$/)[0].authorization).toBe('Bearer e2e-access-token');
});

test('admins rename a category and are told when one in use cannot be deleted', async ({ page, backend }) => {
  backend.on('PATCH', /\/finance\/categories\/cat-food$/, (r) => {
    backend.categories[0] = { ...backend.categories[0], name: r.body.name };
    return { body: backend.categories[0] };
  });
  backend.on('DELETE', /\/finance\/categories\/cat-food$/, () => ({
    status: 409, body: { title: 'Conflict', status: 409, detail: 'This category is used by transactions or a budget. Rename it instead.' },
  }));
  await page.goto('/finance');
  await page.getByRole('tab', { name: 'Categories' }).click();

  const food = page.getByRole('row', { name: /Food/ });
  await food.getByRole('button', { name: 'Rename' }).click();
  await page.getByLabel('New name').fill('Groceries');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText('Category renamed.')).toBeVisible();
  expect(backend.calls('PATCH', /\/finance\/categories\/cat-food$/)[0].body).toEqual({ name: 'Groceries' });

  page.once('dialog', (d) => d.accept());
  await page.getByRole('row', { name: /Groceries/ }).getByRole('button', { name: 'Delete' }).click();
  await expect(page.getByText('409 Conflict – This category is used by transactions or a budget. Rename it instead.')).toBeVisible();
});

test('admins change a budget limit inline and delete a budget', async ({ page, backend }) => {
  let budgets = [{ id: 'b-1', categoryId: 'cat-food', monthlyLimit: 300, currency: 'GBP', periodStart: '2026-11-01', periodEnd: '2026-11-30', spent: 120 }];
  backend.on('GET', /\/budgets$/, () => ({ body: { items: budgets, page: 0, size: 100, totalItems: budgets.length, totalPages: 1 } }));
  backend.on('PATCH', /\/budgets\/b-1$/, (r) => {
    budgets = [{ ...budgets[0], monthlyLimit: r.body.monthlyLimit }];
    return { body: budgets[0] };
  });
  backend.on('DELETE', /\/budgets\/b-1$/, () => {
    budgets = [];
    return { status: 204 };
  });
  await page.goto('/finance');
  await page.getByRole('tab', { name: 'Budgets' }).click();

  await page.getByRole('button', { name: 'Change limit' }).click();
  await page.getByLabel('New monthly limit').fill('450');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText('Budget limit changed.')).toBeVisible();
  expect(backend.calls('PATCH', /\/budgets\/b-1$/)[0].body).toEqual({ monthlyLimit: 450 });

  page.once('dialog', (d) => d.accept());
  await page.getByRole('button', { name: 'Delete' }).click();
  await expect(page.getByText('No budgets yet.')).toBeVisible();
});

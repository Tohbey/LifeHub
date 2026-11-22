import { HOME } from './support/data';
import { expect, test } from './support/fixtures';

test('adds an item to a list and ticks it off', async ({ page, backend }) => {
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
  await page.goto('/shopping');
  await page.getByRole('link', { name: 'Weekly shop' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Weekly shop' })).toBeVisible();

  await page.getByLabel('Item').fill('Milk');
  await page.getByLabel('Quantity').fill('2');
  await page.getByRole('button', { name: 'Add' }).click();

  await expect(page.getByText('Milk')).toBeVisible();
  expect(backend.calls('POST', /\/shopping-lists\/list-1\/items$/)[0].body).toEqual({ name: 'Milk', quantity: '2' });

  await page.getByRole('checkbox', { name: 'Bought Milk' }).check();
  await expect(page.getByText('bought by You')).toBeVisible();
  expect(backend.calls('PATCH', /\/shopping-items\/item-\d+$/)[0].body).toEqual({ purchased: true });
});

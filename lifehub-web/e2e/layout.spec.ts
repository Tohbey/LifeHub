import { HOME, PERSONAL } from './support/data';
import { expect, test } from './support/fixtures';

test('shows the user, the unread badge and every workspace in the switcher', async ({ page, backend }) => {
  backend.unread = 3;
  await page.goto('/');

  await expect(page.getByRole('banner').getByRole('link', { name: '3 unread notifications' })).toContainText('3');
  const switcher = page.getByRole('combobox', { name: 'Workspace' });
  await expect(switcher.locator('option')).toHaveText(['Personal (owner)', 'Fafowora Home (owner)']);
});

test('switching workspace loads that workspace and remembers it', async ({ page, backend }) => {
  await page.goto('/');
  await expect(page.getByText('Your personal workspace')).toBeVisible();

  await page.getByRole('combobox', { name: 'Workspace' }).selectOption(HOME.id);

  await expect(page.getByText('Fafowora Home: you are owner, 2 members')).toBeVisible();
  expect(backend.calls('GET', new RegExp(`/households/${HOME.id}/members$`))).not.toHaveLength(0);
  expect(backend.calls('GET', new RegExp(`/workspaces/${HOME.id}/finance/summary$`))).not.toHaveLength(0);
  expect(await page.evaluate(() => localStorage.getItem('lifehub.workspaceId'))).toBe(HOME.id);

  await page.reload();
  await expect(page.getByText('Fafowora Home: you are owner')).toBeVisible();
  expect(PERSONAL.id).not.toBe(HOME.id);
});

test('navigation reaches every page', async ({ page, isMobile }) => {
  await page.goto('/');
  const nav = page.getByRole('navigation').filter({ visible: true }).first();
  for (const [label, heading] of [['Finance', 'Finance'], ['Tasks', 'Tasks'], ['Calendar', 'Calendar'], ['Shopping', 'Shopping']]) {
    await nav.getByRole('link', { name: label }).click();
    await expect(page.getByRole('heading', { level: 1, name: heading })).toBeVisible();
  }
  if (!isMobile) {
    await nav.getByRole('link', { name: 'Household' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Household' })).toBeVisible();
  }
});

test('the top bar fits the screen and Add works from it', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1, name: /Alice/ })).toBeVisible();

  // No sideways scrolling, on phones too, where Add shrinks to a + icon but keeps its name.
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.getByRole('banner').locator('summary', { hasText: 'Add' }).click();
  await page.getByRole('navigation', { name: 'Add' }).getByRole('link', { name: 'Event' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Calendar' })).toBeVisible();
});

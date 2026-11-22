import { HOME } from './support/data';
import { expect, test } from './support/fixtures';

test.use({ timezoneId: 'Europe/London', locale: 'en-GB' });

test.beforeEach(async ({ page }) => {
  // Thursday 19 November 2026, so the plan shows the week of Monday 16 November.
  await page.clock.setFixedTime(new Date('2026-11-19T10:00:00Z'));
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
});

test('a recipe is added with its ingredients, then planned for dinner', async ({ page, backend }) => {
  await page.goto('/meals');
  await page.getByRole('tab', { name: 'Recipes' }).click();

  await page.getByLabel('Name').fill('Jollof rice');
  await page.getByLabel('Serves').fill('4');
  await page.getByLabel('Quantity').fill('500 g');
  await page.getByLabel('Ingredient 1', { exact: true }).fill('Rice');
  await page.getByRole('button', { name: 'Add ingredient' }).click();
  await page.getByLabel('Ingredient 2', { exact: true }).fill('Tomatoes');
  await page.getByRole('button', { name: 'Add recipe' }).click();

  await expect(page.getByText('Recipe added.')).toBeVisible();
  expect(backend.calls('POST', /\/recipes$/)[0].body).toEqual({ name: 'Jollof rice', servings: 4, instructions: null, sourceUrl: null,
    ingredients: [{ name: 'Rice', quantity: '500 g' }, { name: 'Tomatoes', quantity: null }] });
  await expect(page.getByText('500 g Rice, Tomatoes')).toBeVisible();

  await page.getByRole('tab', { name: 'Week plan' }).click();
  await page.getByLabel('Day').selectOption({ label: 'Fri 20 Nov' });
  await page.getByLabel('Recipe').selectOption({ label: 'Jollof rice' });
  await page.getByRole('button', { name: 'Add to plan' }).click();

  await expect(page.getByText('Meal planned.')).toBeVisible();
  expect(backend.calls('POST', /\/meals$/)[0].body).toMatchObject({ date: '2026-11-20', slot: 'DINNER' });
  const friday = page.getByRole('row', { name: /Fri 20 Nov/ });
  await expect(friday.getByRole('cell').nth(2)).toContainText('Jollof rice');
  expect(backend.calls('GET', /\/meals$/)[0].query.get('from')).toBe('2026-11-16');
});

test('a free-text meal can be planned and removed, and weeks can be paged', async ({ page, backend }) => {
  await page.goto('/meals');

  await page.getByLabel('Meal').selectOption('BREAKFAST');
  await page.getByLabel('What are you eating?').fill('Pancakes');
  await page.getByRole('button', { name: 'Add to plan' }).click();
  await page.getByRole('button', { name: /Remove Pancakes on/ }).click();
  expect(backend.calls('DELETE', /\/meals\/meal-/)).toHaveLength(1);

  await page.getByRole('button', { name: 'Next week' }).click();
  await expect(page.getByRole('heading', { name: 'Week of Mon 23 Nov' })).toBeVisible();
  expect(backend.calls('GET', /\/meals$/).at(-1)!.query.get('to')).toBe('2026-11-29');
});

test("a recipe's ingredients go onto a shopping list in one go", async ({ page, backend }) => {
  backend.recipes = [{ id: 'r1', name: 'Pancakes', servings: 2, instructions: null, sourceUrl: null,
    ingredients: [{ name: 'Flour', quantity: '200 g' }, { name: 'Eggs', quantity: '2' }], createdByUserId: backend.me.id }];
  await page.goto('/meals');
  await page.getByRole('tab', { name: 'Recipes' }).click();

  await page.getByRole('button', { name: 'Add to list' }).click();

  await expect(page.getByText('Added 2 ingredients to Weekly shop.')).toBeVisible();
  expect(backend.calls('POST', /\/recipes\/r1\/shopping-list$/)[0].body).toEqual({ shoppingListId: 'list-1' });
});

import { HOME, PERSONAL } from './support/data';
import { expect, test } from './support/fixtures';

test('notes are written, shared by default in a household, tagged and filtered', async ({ page, backend }) => {
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
  await page.goto('/notes');

  await page.getByLabel('Title').fill('Wi-Fi');
  await page.getByLabel('Text').fill('Network: Home\nPassword: hunter2');
  await page.getByLabel('Tags (comma separated)').fill('Home, admin');
  await page.getByLabel('Pin to the top').check();
  await page.getByRole('button', { name: 'Add note' }).click();

  await expect(page.getByText('Note added.')).toBeVisible();
  expect(backend.calls('POST', /\/notes$/)[0].body).toEqual({ title: 'Wi-Fi', body: 'Network: Home\nPassword: hunter2', pinned: true,
    visibility: 'SHARED', tags: ['Home', 'admin'] });
  const note = page.getByRole('article', { name: 'Wi-Fi' });
  await expect(note).toContainText('Pinned');
  await expect(note).toContainText('home');

  await page.getByLabel('Title').fill('Diary');
  await page.getByLabel('Share with the household').uncheck();
  await page.getByRole('button', { name: 'Add note' }).click();
  await expect(page.getByRole('article', { name: 'Diary' })).toContainText('Only you');

  await page.getByRole('group', { name: 'Filter by tag' }).getByRole('button', { name: /admin/ }).click();
  await expect(page.getByRole('article')).toHaveCount(1);
  expect(backend.calls('GET', /\/notes$/).at(-1)!.query.get('tag')).toBe('admin');
});

test('in a personal workspace there is nothing to share', async ({ page, backend }) => {
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), PERSONAL.id);
  await page.goto('/notes');

  await expect(page.getByLabel('Share with the household')).toHaveCount(0);
  await page.getByLabel('Title').fill('Ideas');
  await page.getByRole('button', { name: 'Add note' }).click();
  expect(backend.calls('POST', /\/notes$/)[0].body.visibility).toBe('PERSONAL');
});

test('links open safely in a new tab and can be edited and deleted', async ({ page, backend }) => {
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
  await page.goto('/notes');
  await page.getByRole('tab', { name: 'Links' }).click();

  await page.getByLabel('Link').fill('https://www.gov.uk/bin-collection');
  await page.getByLabel('Tags (comma separated)').fill('bins');
  await page.getByRole('button', { name: 'Save link' }).click();

  const link = page.getByRole('link', { name: 'www.gov.uk' });
  await expect(link).toHaveAttribute('href', 'https://www.gov.uk/bin-collection');
  await expect(link).toHaveAttribute('target', '_blank');
  await expect(link).toHaveAttribute('rel', 'noopener noreferrer');

  await page.getByRole('button', { name: 'Edit www.gov.uk' }).click();
  await page.getByLabel('Title (optional)').fill('Bin days');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('link', { name: 'Bin days' })).toBeVisible();
  expect(backend.calls('PUT', /\/bookmarks\/link-/)[0].body).toMatchObject({ title: 'Bin days', tags: ['bins'] });

  page.once('dialog', (d) => d.accept());
  await page.getByRole('button', { name: 'Delete Bin days' }).click();
  await expect(page.getByText('No links yet.')).toBeVisible();
});

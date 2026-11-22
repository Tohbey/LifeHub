import { HOME } from './support/data';
import { expect, test } from './support/fixtures';

test.beforeEach(async ({ page }) => {
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
});

test('admins see pending invitations and can revoke one', async ({ page, backend }) => {
  backend.pendingInvitations = [
    { id: 'inv-1', email: 'sam@example.com', role: 'MEMBER', invitedByUserId: backend.me.id, createdAt: '2026-11-17T09:00:00Z', expiresAt: '2026-11-24T09:00:00Z' },
  ];
  await page.goto('/household');

  const row = page.getByRole('row', { name: /sam@example.com/ });
  await expect(row).toContainText('You');
  page.once('dialog', (d) => d.accept());
  await row.getByRole('button', { name: 'Revoke' }).click();

  await expect(page.getByText('Invitation revoked.')).toBeVisible();
  await expect(page.getByText('No pending invitations.')).toBeVisible();
  expect(backend.calls('DELETE', new RegExp(`/households/${HOME.id}/invitations/inv-1$`))).toHaveLength(1);
});

test('members do not see pending invitations', async ({ page, backend }) => {
  backend.workspaces = [{ ...HOME, role: 'MEMBER' }];
  await page.goto('/household');
  await expect(page.getByRole('heading', { name: 'Members' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Pending invitations' })).toHaveCount(0);
  expect(backend.calls('GET', /\/invitations$/)).toHaveLength(0);
});

import { BOB, HOME } from './support/data';
import { expect, test } from './support/fixtures';

test('admins see the audit log with each event payload', async ({ page, backend }) => {
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
  backend.on('GET', /\/audit$/, () => ({
    body: {
      items: [{ eventId: 'e1', type: 'transaction.created', version: 1, occurredAt: '2026-11-16T10:00:00Z',
        actorUserId: 'gone-user', actorName: 'Carol',
        payload: { description: 'Electricity', amount: '120.00', payerUserId: 'gone-user', splits: [{ userId: BOB.id, amount: '60.00' }] },
        people: { 'gone-user': 'Carol', [BOB.id]: 'Bob' } }],
      page: 0, size: 50, totalItems: 1, totalPages: 1,
    },
  }));
  await page.goto('/');
  await page.getByRole('link', { name: 'Audit log' }).click();

  await expect(page.getByText('transaction.created')).toBeVisible();
  // Carol has left the household, so only the audit service still knows her name.
  await expect(page.getByRole('cell', { name: 'Carol' })).toBeVisible();
  await expect(page.getByText('People: Bob')).toBeVisible();
  await page.getByText('show').click();
  await expect(page.getByText('"payerUserId": "Carol (gone-user)"')).toBeVisible();
  await expect(page.getByText(`"userId": "Bob (${BOB.id})"`)).toBeVisible();
});

test('members do not get the audit log link', async ({ page, backend }) => {
  backend.workspaces = [{ ...HOME, role: 'MEMBER' }];
  await page.goto('/');
  await expect(page.getByRole('link', { name: 'Finance' }).first()).toBeVisible();
  await expect(page.getByRole('link', { name: 'Audit log' })).toHaveCount(0);
});

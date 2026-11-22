import { HOME } from './support/data';
import { expect, test } from './support/fixtures';

const EXPENSE = {
  id: 'n-1', workspaceId: HOME.id, type: 'SHARED_EXPENSE', title: 'New shared expense: Electricity',
  body: 'Bob added Electricity (120.00 GBP). Your share is 60.00 GBP.', link: '/finance/transactions/tx-1',
  count: 1, read: false, createdAt: '2026-11-16T10:00:00Z',
};

test('a notification arriving on the live stream shows a toast and updates the badge', async ({ page, backend }) => {
  backend.streamEvents(EXPENSE).on('GET', /\/notifications\/stream$/, () => {
    // After the stream delivers, the server has one unread notification.
    backend.unread = 1;
    backend.notifications = [EXPENSE];
    return { contentType: 'text/event-stream', raw: `event:notification\ndata:${JSON.stringify(EXPENSE)}\n\n` };
  });
  await page.goto('/');

  await expect(page.getByText('New shared expense: Electricity: Bob added Electricity')).toBeVisible();
  await expect(page.getByRole('link', { name: '1 unread notifications' })).toBeVisible();
  expect(backend.calls('GET', /\/notifications\/stream$/)[0].authorization).toBe('Bearer e2e-access-token');
});

test('lists notifications, opens one in its workspace, and marks all read', async ({ page, backend }) => {
  backend.notifications = [EXPENSE, { ...EXPENSE, id: 'n-2', read: true, title: 'Task done: Bins', type: 'TASK_COMPLETED', link: null }];
  backend.unread = 1;
  backend.on('PATCH', /\/me\/notifications\/n-1$/, (r) => {
    backend.notifications[0] = { ...EXPENSE, read: r.body.read };
    backend.unread = 0;
    return { body: backend.notifications[0] };
  });
  await page.goto('/notifications');

  await expect(page.getByText('Task done: Bins')).toBeVisible();
  await page.getByText('New shared expense: Electricity').click();

  await expect(page).toHaveURL(/\/finance$/);
  await expect(page.getByRole('combobox', { name: 'Workspace' })).toHaveValue(HOME.id);
  expect(backend.calls('PATCH', /\/me\/notifications\/n-1$/)[0].body).toEqual({ read: true });

  await page.goto('/notifications');
  await page.getByRole('button', { name: 'Mark all read' }).click();
  await expect(page.getByText('0 unread')).toBeVisible();
});

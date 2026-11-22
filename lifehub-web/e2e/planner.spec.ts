import { BOB, HOME } from './support/data';
import { expect, test } from './support/fixtures';

test.beforeEach(async ({ page }) => {
  await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
});

test('creates a task for another member and completes it', async ({ page, backend }) => {
  await page.goto('/tasks');

  await page.getByLabel('Title').fill('Take the bins out');
  await page.getByLabel('Assign to').selectOption({ label: 'Bob' });
  await page.getByRole('button', { name: 'Add task' }).click();

  await expect(page.getByText('Task added; the assignee is notified.')).toBeVisible();
  expect(backend.calls('POST', /\/tasks$/)[0].body).toMatchObject({ title: 'Take the bins out', assigneeUserId: BOB.id });

  await page.getByRole('checkbox', { name: 'Complete Take the bins out' }).check();
  await expect.poll(() => backend.calls('PATCH', /\/tasks\/task-\d+$/).map((r) => r.body)).toEqual([{ status: 'DONE' }]);
});

test('a task cannot be due in the past', async ({ page, backend }) => {
  await page.goto('/tasks');

  const due = page.getByLabel('Due');
  const min = await due.getAttribute('min');
  expect(new Date(min!).getTime()).toBeGreaterThan(Date.now() - 120_000);

  await page.getByLabel('Title').fill('Too late');
  await due.fill('2020-01-01T09:00');
  await expect(page.getByText('The due date cannot be in the past.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Add task' })).toBeDisabled();
  expect(backend.calls('POST', /\/tasks$/)).toHaveLength(0);
});

test('adds an event with attendees and a reminder', async ({ page, backend }) => {
  backend.on('POST', /\/events$/, (r) => ({ status: 201, body: { id: 'ev-1', status: 'ACTIVE', createdByUserId: backend.me.id, ...r.body } }));
  await page.goto('/calendar');

  await page.getByLabel('Title').fill("Parents' evening");
  await page.getByLabel('Bob').check();
  await page.getByLabel('Remind').selectOption({ label: '1 hour before' });
  await page.getByRole('button', { name: 'Add event' }).click();

  await expect(page.getByText('Event added; attendees are notified.')).toBeVisible();
  const body = backend.calls('POST', /\/events$/)[0].body;
  expect(body).toMatchObject({ title: "Parents' evening", allDay: false, reminderMinutesBefore: 60 });
  expect(body.attendeeUserIds.sort()).toEqual([backend.me.id, BOB.id].sort());
  expect(new Date(body.endsAt).getTime()).toBeGreaterThan(new Date(body.startsAt).getTime());
});

test('the creator deletes a task', async ({ page, backend }) => {
  backend.tasks = [{ id: 't-9', title: 'Old chore', status: 'OPEN', assigneeUserId: null, dueAt: null, description: null,
    completedAt: null, completedByUserId: null, createdByUserId: backend.me.id, createdAt: '2026-11-10T09:00:00Z' }];
  await page.goto('/tasks');

  page.once('dialog', (d) => d.accept());
  await page.getByRole('button', { name: 'Delete Old chore' }).click();

  await expect(page.getByText('Task deleted.')).toBeVisible();
  expect(backend.calls('DELETE', /\/tasks\/t-9$/)).toHaveLength(1);
});

test('a repeating event sends its rule, and a start in the past is refused', async ({ page, backend }) => {
  backend.on('POST', /\/events$/, (r) => ({ status: 201, body: { id: 'ev-2', status: 'ACTIVE', createdByUserId: backend.me.id, ...r.body } }));
  await page.goto('/calendar');

  await page.getByLabel('Title').fill('Football');
  await page.getByLabel('Repeat').selectOption('WEEKLY');
  await page.getByLabel('Every').fill('2');
  await page.getByLabel('Until (optional)').fill('2027-06-30');
  await page.getByRole('button', { name: 'Add event' }).click();

  await expect(page.getByText('Event added; attendees are notified.')).toBeVisible();
  expect(backend.calls('POST', /\/events$/)[0].body.recurrence).toEqual({ frequency: 'WEEKLY', interval: 2, until: '2027-06-30' });

  await page.getByLabel('Starts').fill('2020-01-01T09:00');
  await expect(page.getByText('An event cannot start in the past.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Add event' })).toBeDisabled();
});

test('a repeating event shows how it repeats', async ({ page, backend }) => {
  backend.on('GET', /\/events$/, () => ({ body: { items: [{ id: 'ev-3', title: 'Bin day', description: null, location: null,
    startsAt: new Date(Date.now() + 86400000).toISOString(), endsAt: new Date(Date.now() + 90000000).toISOString(),
    allDay: false, reminderMinutesBefore: null, status: 'ACTIVE', attendeeUserIds: [], createdByUserId: backend.me.id,
    recurrence: { frequency: 'WEEKLY', interval: 1, until: null } }], page: 0, size: 100, totalItems: 1, totalPages: 1 } }));
  await page.goto('/calendar');
  await expect(page.getByText('Repeats weekly')).toBeVisible();
});

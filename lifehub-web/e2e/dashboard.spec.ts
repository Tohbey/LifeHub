import { problem } from './support/data';
import { expect, test } from './support/fixtures';

test('each card shows its own data, and a failing service only breaks its own card', async ({ page, backend }) => {
  backend.on('GET', /\/finance\/summary$/, () => ({
    status: 503, body: problem(503, 'Service Unavailable', 'Access could not be checked right now. Try again shortly.'),
  }));
  backend.tasks = [{ id: 't1', title: 'Pay electricity bill', status: 'OPEN', assigneeUserId: null, dueAt: null,
    description: null, completedAt: null, completedByUserId: null, createdByUserId: backend.me.id, createdAt: '2026-11-10T09:00:00Z' }];

  await page.goto('/');

  await expect(page.getByText('503 Service Unavailable – Access could not be checked right now. Try again shortly.')).toBeVisible();
  await expect(page.getByText('Pay electricity bill')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Weekly shop' })).toBeVisible();
});

/*
 * The redesigned home page. The browser's clock is fixed at 10:00 on Thursday 19 November 2026 in London, so the
 * day strip, "Now" and "overdue" give the same answer whenever the suite runs.
 */
test.describe('home page', () => {
  test.use({ timezoneId: 'Europe/London', locale: 'en-GB' });

  const NOW = new Date('2026-11-19T10:00:00Z');
  const page1 = (items: unknown[]) => ({ body: { items, page: 0, size: 100, totalItems: items.length, totalPages: 1 } });
  const task = (id: string, title: string, dueAt: string | null) => ({ id, title, status: 'OPEN', assigneeUserId: null, dueAt,
    description: null, completedAt: null, completedByUserId: null, createdByUserId: 'u-alice', createdAt: '2026-11-10T09:00:00Z' });
  const event = (id: string, title: string, startsAt: string, endsAt: string, status = 'ACTIVE') => ({ id, title, startsAt, endsAt,
    status, allDay: false, location: null, description: null, reminderMinutesBefore: null, attendeeUserIds: [],
    createdByUserId: 'u-alice', recurrence: null });
  const budget = (id: string, spent: number, monthlyLimit: number) => ({ id, categoryId: 'cat-food', currency: 'GBP', spent, monthlyLimit,
    periodStart: '2026-11-01', periodEnd: '2026-11-30' });

  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(NOW);
  });

  test('the day strip shows today in time order, overdue tasks above it, and later events under Coming up', async ({ page, backend }) => {
    backend.tasks = [task('t1', 'Put the bins out', '2026-11-19T19:00:00Z'), task('t2', 'Renew car insurance', '2026-11-16T09:00:00Z'),
      task('t3', 'Book dentist', null)];
    backend.on('GET', /\/events$/, () => page1([
      event('e2', 'Plumber visit', '2026-11-19T13:00:00Z', '2026-11-19T14:00:00Z'),
      event('e1', 'School run', '2026-11-19T08:15:00Z', '2026-11-19T09:00:00Z'),
      event('e3', 'Cancelled lunch', '2026-11-19T12:00:00Z', '2026-11-19T13:00:00Z', 'CANCELLED'),
      event('e4', "Parents' evening", '2026-11-20T17:30:00Z', '2026-11-20T19:00:00Z'),
    ]));
    await page.goto('/');

    const today = page.getByRole('region', { name: 'Thursday 19 November' });
    await expect(today.getByRole('listitem')).toHaveText([/08:15\s*School run/, /13:00\s*Plumber visit/, /19:00\s*Put the bins out/]);
    await expect(today.getByText('Overdue:')).toBeVisible();
    await expect(today.getByRole('link', { name: 'Renew car insurance' })).toBeVisible();
    await expect(today.getByText('Now')).toBeVisible();
    await expect(today.getByText('Cancelled lunch')).toHaveCount(0);

    const comingUp = page.getByRole('region', { name: 'Coming up' });
    await expect(comingUp).toContainText("Parents' evening");
    await expect(comingUp).not.toContainText('School run');

    await today.getByRole('link', { name: /Plumber visit/ }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Calendar' })).toBeVisible();
  });

  test('an empty day invites planning, and nothing waiting says so', async ({ page, backend }) => {
    backend.tasks = [];
    backend.unread = 0;
    await page.goto('/');

    await expect(page.getByText('Nothing scheduled or due today.')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Plan something' })).toHaveAttribute('href', '/calendar');
    await expect(page.getByRole('region', { name: 'Needs attention' })).toContainText('Nothing is waiting on you.');
  });

  test('needs attention lists budgets at 80% or more, overdue tasks and unread notifications', async ({ page, backend }) => {
    backend.unread = 2;
    backend.tasks = [task('t2', 'Renew car insurance', '2026-11-16T09:00:00Z')];
    backend.on('GET', /\/budgets$/, () => page1([budget('b1', 276, 300), budget('b2', 60, 200)]));
    await page.goto('/');

    const attention = page.getByRole('region', { name: 'Needs attention' });
    await expect(attention.getByRole('listitem')).toHaveText([
      'Food budget is at 92% (£276.00 of £300.00)',
      '1 task is overdue',
      '2 unread notifications',
    ]);
    await attention.getByRole('link', { name: '1 task is overdue' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Tasks' })).toBeVisible();
  });

  test('this month shows the money and one bar for all budgets in a currency', async ({ page, backend }) => {
    backend.on('GET', /\/finance\/summary$/, () => ({ body: { from: '2026-11-01', to: '2026-11-30',
      currencies: [{ currency: 'GBP', income: 2000, expense: 750.5, net: 1249.5 }], byCategory: [] } }));
    backend.on('GET', /\/budgets$/, () => page1([budget('b1', 276, 300), budget('b2', 60, 200)]));
    await page.goto('/');

    const month = page.getByRole('region', { name: 'This month' });
    await expect(month.getByRole('definition')).toHaveText(['£750.50', '£2,000.00', '£1,249.50']);
    await expect(month).toContainText('£336.00 of £500.00 budgeted');
    await expect(month.getByRole('img', { name: '67% of the GBP budgets used' })).toBeVisible();
  });

  test('the Add menu opens the page for each kind of thing', async ({ page }) => {
    await page.goto('/');

    for (const [item, heading] of [['Expense or income', 'Finance'], ['Task', 'Tasks'], ['Event', 'Calendar'], ['Shopping item', 'Shopping']]) {
      await page.getByRole('banner').locator('summary', { hasText: 'Add' }).click();
      await page.getByRole('navigation', { name: 'Add' }).getByRole('link', { name: item }).click();
      await expect(page.getByRole('heading', { level: 1, name: heading })).toBeVisible();
      await expect(page.getByRole('navigation', { name: 'Add' })).toBeHidden();
    }
  });
});

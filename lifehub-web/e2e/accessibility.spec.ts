import AxeBuilder from '@axe-core/playwright';

import { HOME } from './support/data';
import { expect, test } from './support/fixtures';

/**
 * Scans every page with axe (WCAG 2.1 A and AA rules) and fails on serious or critical problems: missing labels,
 * colour contrast, names on buttons and links, landmark structure and so on.
 */
const PAGES: [string, string | RegExp][] = [
  ['/', /Alice/],
  ['/finance', 'Finance'],
  ['/tasks', 'Tasks'],
  ['/calendar', 'Calendar'],
  ['/shopping', 'Shopping'],
  ['/meals', 'Meals'],
  ['/notes', 'Notes'],
  ['/household', 'Household'],
  ['/notifications', 'Notifications'],
  ['/audit', 'Audit log'],
  ['/profile', 'Profile'],
];

for (const scheme of ['light', 'dark'] as const) {
  test.describe(`${scheme} mode`, () => {
    test.use({ colorScheme: scheme });

    for (const [path, heading] of PAGES) {
      test(`${path} has no serious accessibility problems`, async ({ page, backend }) => {
        await page.addInitScript((id) => localStorage.setItem('lifehub.workspaceId', id), HOME.id);
        backend.tasks = [{ id: 't1', title: 'Pay bill', status: 'OPEN', assigneeUserId: null, dueAt: null, description: null,
          completedAt: null, completedByUserId: null, createdByUserId: backend.me.id, createdAt: '2026-11-10T09:00:00Z' }];
        await page.goto(path);
        await expect(page.getByRole('heading', { level: 1, name: heading })).toBeVisible();
        await page.waitForTimeout(300);

        const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
        const serious = results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
        expect(serious.map((v) => `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(' ')).slice(0, 3).join(' | ')})`))
          .toEqual([]);
      });
    }
  });
}

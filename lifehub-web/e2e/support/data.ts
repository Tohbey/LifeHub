/** Test data shared by the specs: two users, a personal workspace and a household. */

export const ALICE = { id: '01930000-0000-7000-8000-00000000a11c', displayName: 'Alice', email: 'alice@example.com', createdAt: '2026-11-01T09:00:00Z' };
export const BOB = { id: '01930000-0000-7000-8000-000000000b0b', displayName: 'Bob', email: 'bob@example.com', createdAt: '2026-11-02T09:00:00Z' };

export const PERSONAL = { id: '01930000-0000-7000-8000-0000000000a1', type: 'PERSONAL', name: 'Personal', role: 'OWNER' };
export const HOME = { id: '01930000-0000-7000-8000-000000000e0e', type: 'HOUSEHOLD', name: 'Fafowora Home', role: 'OWNER' };

export const MEMBERS = [
  { userId: ALICE.id, displayName: 'Alice', role: 'OWNER', joinedAt: '2026-11-01T09:00:00Z' },
  { userId: BOB.id, displayName: 'Bob', role: 'MEMBER', joinedAt: '2026-11-03T09:00:00Z' },
];

/** A page of items, as every LifeHub list endpoint returns it. */
export function page<T>(items: T[]) {
  return { items, page: 0, size: 20, totalItems: items.length, totalPages: items.length ? 1 : 0 };
}

/** A ProblemDetail error body. */
export function problem(status: number, title: string, detail: string, errors?: Record<string, string>) {
  return { type: 'about:blank', title, status, detail, ...(errors ? { errors } : {}) };
}

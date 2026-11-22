import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';

import { IdentityApi } from '../api/identity.api';
import { AuthService } from '../auth/auth.service';
import { WorkspaceService } from './workspace.service';

const ME = { id: 'me', displayName: null, email: null, emailVerified: false, createdAt: '2026-11-01T00:00:00Z' };
const HOME = { id: 'home', type: 'HOUSEHOLD' as const, name: 'Home', role: 'ADMIN' as const };

describe('WorkspaceService', () => {
  let identity: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    identity = {
      me: vi.fn().mockResolvedValue(ME),
      updateMe: vi.fn().mockImplementation(async (body) => ({ ...ME, ...body })),
      workspaces: vi.fn().mockResolvedValue({ items: [HOME], page: 0, size: 100, totalItems: 1, totalPages: 1 }),
      members: vi.fn().mockResolvedValue({ items: [
        { userId: 'me', displayName: 'Alice', role: 'ADMIN', joinedAt: '' },
        { userId: 'bob', displayName: null, role: 'MEMBER', joinedAt: '' },
      ] }),
      household: vi.fn().mockResolvedValue({ id: 'home', name: 'Home', defaultCurrency: 'NGN', timezone: 'Africa/Lagos' }),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: IdentityApi, useValue: identity },
        { provide: AuthService, useValue: { claims: signal({ sub: 's', name: 'Alice' }) } },
      ],
    });
    localStorage.clear();
  });

  it('fills a missing name from the sign-in, loads the household and its currency', async () => {
    const ws = TestBed.inject(WorkspaceService);
    await ws.load();

    expect(identity['updateMe']).toHaveBeenCalledWith({ displayName: 'Alice' });
    expect(ws.me()?.displayName).toBe('Alice');
    expect(ws.current()?.id).toBe('home');
    expect(ws.defaultCurrency()).toBe('NGN');
    expect(ws.isAdmin()).toBe(true);
    expect(ws.hasRole('OWNER')).toBe(false);
  });

  it('names people: you, members, unnamed members and strangers', async () => {
    const ws = TestBed.inject(WorkspaceService);
    await ws.load();

    expect(ws.name('me')).toBe('You');
    expect(ws.name('bob')).toBe('Unnamed member');
    expect(ws.name('0193abcd-0000-7000-8000-000000000000')).toBe('0193abcd…');
    expect(ws.name(null)).toBe('–');
  });
});

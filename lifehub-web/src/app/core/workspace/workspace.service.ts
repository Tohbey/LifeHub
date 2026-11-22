import { Injectable, computed, inject, signal } from '@angular/core';

import { IdentityApi, Member, User, Workspace } from '../api/identity.api';
import { AuthService } from '../auth/auth.service';
import { ROLE_RANK, WorkspaceRole } from '../api/models';

const SELECTED_KEY = 'lifehub.workspaceId';

/**
 * Who is signed in and which workspace they are looking at. Every feature page works on {@link current}.
 * The selection is remembered per browser.
 */
@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly identity = inject(IdentityApi);
  private readonly auth = inject(AuthService);

  readonly me = signal<User | null>(null);
  readonly workspaces = signal<Workspace[]>([]);
  readonly members = signal<Member[]>([]);
  /** The currency new money in this workspace defaults to: the household's own, or GBP for a personal workspace. */
  readonly defaultCurrency = signal('GBP');
  private readonly selectedId = signal<string | null>(readSelection());

  /** The workspace in use: the remembered one, or the first (personal) one. */
  readonly current = computed<Workspace | null>(() => {
    const list = this.workspaces();
    return list.find((w) => w.id === this.selectedId()) ?? list[0] ?? null;
  });

  /** True when the caller is at least an admin of the current workspace. */
  readonly isAdmin = computed(() => this.hasRole('ADMIN'));

  /**
   * Loads the user (creating them on first sign-in), their workspaces and the current workspace's members.
   * A profile with no name gets the name (or username) the user signed up to Logto with, so other people,
   * notifications and the audit log show a name instead of an ID.
   */
  async load(): Promise<void> {
    this.me.set(await this.identity.me());
    await this.fillNameFromSignIn();
    await this.reloadWorkspaces();
  }

  /** PATCH /me with the Logto name when the profile has none. Best effort: a failure leaves the profile as it is. */
  private async fillNameFromSignIn(): Promise<void> {
    const me = this.me();
    const claims = this.auth.claims();
    const name = (claims?.name || claims?.username || '').trim().slice(0, 100);
    if (!me || me.displayName || !name) {
      return;
    }
    try {
      this.me.set(await this.identity.updateMe({ displayName: name }));
    } catch {
      // The user can still set a name on the profile page.
    }
  }

  /** Re-reads the workspace list, for example after creating or joining a household. */
  async reloadWorkspaces(): Promise<void> {
    this.workspaces.set((await this.identity.workspaces()).items);
    await this.reloadMembers();
  }

  /** Switches to another workspace. */
  async select(id: string): Promise<void> {
    this.selectedId.set(id);
    try {
      localStorage.setItem(SELECTED_KEY, id);
    } catch {
      // Private mode: the choice is just not remembered.
    }
    await this.reloadMembers();
  }

  /** Re-reads the members of the current workspace. A personal workspace has only its owner. */
  async reloadMembers(): Promise<void> {
    const ws = this.current();
    const me = this.me();
    if (!ws || !me) {
      this.members.set([]);
      return;
    }
    if (ws.type === 'PERSONAL') {
      this.members.set([{ userId: me.id, displayName: me.displayName ?? 'You', role: 'OWNER', joinedAt: me.createdAt }]);
      this.defaultCurrency.set('GBP');
      return;
    }
    const [members, household] = await Promise.all([this.identity.members(ws.id), this.identity.household(ws.id)]);
    this.members.set(members.items);
    this.defaultCurrency.set(household.defaultCurrency);
  }

  /** A member's name for display, "You" for the caller, or a short ID for unknown users. */
  name(userId: string | null | undefined): string {
    if (!userId) {
      return '–';
    }
    if (userId === this.me()?.id) {
      return 'You';
    }
    const member = this.members().find((m) => m.userId === userId);
    return member?.displayName || (member ? 'Unnamed member' : `${userId.slice(0, 8)}…`);
  }

  /** True when the caller's role in the current workspace is at least {@code role}. */
  hasRole(role: WorkspaceRole): boolean {
    const ws = this.current();
    return !!ws && ROLE_RANK[ws.role] >= ROLE_RANK[role];
  }
}

/** The remembered workspace ID, if storage is available. */
function readSelection(): string | null {
  try {
    return localStorage.getItem(SELECTED_KEY);
  } catch {
    return null;
  }
}

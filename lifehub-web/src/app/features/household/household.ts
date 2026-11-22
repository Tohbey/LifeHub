import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { Household, IdentityApi, Invitation, Member, PendingInvitation } from '../../core/api/identity.api';
import { WorkspaceRole } from '../../core/api/models';
import { ReferenceDataStore } from '../../core/api/reference.api';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { dateTime } from '../../shared/format';
import { Avatar } from '../../shared/avatar';
import { ToastService } from '../../shared/toast.service';

/**
 * Households (identity-workspace): create one, see and change the current one, manage members and roles,
 * invite people, and join one with an invite link.
 */
@Component({
  selector: 'lh-household',
  imports: [FormsModule, Avatar],
  template: `
    <header class="page-head"><div class="grow"><h1>Household</h1><p class="subtitle">Members, roles and invitations.</p></div></header>

    @if (household(); as h) {
      <section class="card">
        <h2>{{ h.name }}</h2>
        <p class="muted">Created {{ dateTime(h.createdAt) }} · you are {{ h.yourRole.toLowerCase() }}</p>
        <form class="form inline" (ngSubmit)="saveSettings()">
          <label>Name <input name="name" [(ngModel)]="settings.name" [disabled]="!ws.isAdmin()" maxlength="100" /></label>
          <label>Currency
            <select name="currency" [(ngModel)]="settings.defaultCurrency" [disabled]="!ws.isAdmin()">
              @for (c of ref.currencies(); track c.code) { <option [value]="c.code">{{ ref.currencyLabel(c) }}</option> }
            </select>
          </label>
          <label>Timezone
            <select name="timezone" [(ngModel)]="settings.timezone" [disabled]="!ws.isAdmin()">
              @for (z of ref.timezones(); track z.id) { <option [value]="z.id">{{ ref.timezoneLabel(z) }}</option> }
            </select>
          </label>
          @if (ws.isAdmin()) { <button class="button">Save</button> }
        </form>
      </section>

      <section class="card">
        <h2>Members</h2>
        <table class="table">
          <thead><tr><th>Name</th><th>Role</th><th>Joined</th><th></th></tr></thead>
          <tbody>
            @for (m of members(); track m.userId) {
              <tr>
                <td><div class="person"><lh-avatar [name]="m.displayName" [size]="34" />
                  <div>{{ ws.name(m.userId) }}<br /><code class="member-id">{{ m.userId }}</code></div></div></td>
                <td>
                  @if (h.yourRole === 'OWNER' && m.userId !== ws.me()?.id) {
                    <select [ngModel]="m.role" (ngModelChange)="changeRole(m, $event)" [name]="'role-' + m.userId"
                            [attr.aria-label]="'Role of ' + ws.name(m.userId)">
                      <option value="MEMBER">Member</option><option value="ADMIN">Admin</option><option value="OWNER">Owner (hand over)</option>
                    </select>
                  } @else { {{ m.role.toLowerCase() }} }
                </td>
                <td>{{ dateTime(m.joinedAt) }}</td>
                <td class="actions">
                  @if (m.userId === ws.me()?.id) {
                    <button class="button ghost danger" (click)="remove(m, true)">Leave</button>
                  } @else {
                    <button class="button ghost danger" (click)="remove(m, false)">Remove</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </section>

      <section class="card">
        <h2>Invite someone</h2>
        <form class="form inline" (ngSubmit)="invite()">
          <label>Email <input name="inviteEmail" type="email" required [(ngModel)]="inviteEmail" /></label>
          <label>Role
            <select name="inviteRole" [(ngModel)]="inviteRole">
              <option value="MEMBER">Member</option><option value="ADMIN">Admin</option>
            </select>
          </label>
          <button class="button" [disabled]="busy()">Create invite link</button>
        </form>
        @if (invitation(); as inv) {
          <div class="callout">
            <p>Send this link to {{ inv.email }}. It works once and expires {{ dateTime(inv.expiresAt) }}. It is shown only now.</p>
            <code class="copyable">{{ inviteLink(inv) }}</code>
            <button class="button ghost" (click)="copy(inviteLink(inv))">Copy link</button>
          </div>
        }
      </section>

      @if (ws.isAdmin()) {
        <section class="card">
          <h2>Pending invitations</h2>
          <table class="table">
            <thead><tr><th>Email</th><th>Role</th><th>Invited by</th><th>Expires</th><th></th></tr></thead>
            <tbody>
              @for (inv of pending(); track inv.id) {
                <tr>
                  <td>{{ inv.email }}</td>
                  <td>{{ inv.role.toLowerCase() }}</td>
                  <td>{{ ws.name(inv.invitedByUserId) }}</td>
                  <td>{{ dateTime(inv.expiresAt) }}</td>
                  <td class="actions"><button class="button ghost danger" (click)="revoke(inv)">Revoke</button></td>
                </tr>
              } @empty { <tr><td colspan="5" class="muted">No pending invitations.</td></tr> }
            </tbody>
          </table>
        </section>
      }
    } @else if (ws.current()?.type === 'PERSONAL') {
      <section class="card">
        <p>You are in your personal workspace. Create a household to share money, tasks and shopping with others,
          or switch to a household you belong to with the switcher at the top.</p>
      </section>
    }

    <section class="card">
      <h2>Create a household</h2>
      <form class="form inline" (ngSubmit)="create()">
        <label>Name <input name="newName" required maxlength="100" [(ngModel)]="newName" /></label>
        <label>Currency
          <select name="newCurrency" [(ngModel)]="newCurrency">
            @for (c of ref.currencies(); track c.code) { <option [value]="c.code">{{ ref.currencyLabel(c) }}</option> }
          </select>
        </label>
        <label>Timezone
          <select name="newTimezone" [(ngModel)]="newTimezone">
            @for (z of ref.timezones(); track z.id) { <option [value]="z.id">{{ ref.timezoneLabel(z) }}</option> }
          </select>
        </label>
        <button class="button" [disabled]="busy()">Create</button>
      </form>
    </section>

    <section class="card">
      <h2>Join with an invite link</h2>
      <form class="form inline" (ngSubmit)="openInvite()">
        <label class="grow">Invite link or token <input name="inviteInput" [(ngModel)]="inviteInput" required /></label>
        <button class="button">Open</button>
      </form>
    </section>
  `,
})
export class HouseholdPage implements OnInit {
  protected readonly ws = inject(WorkspaceService);
  protected readonly ref = inject(ReferenceDataStore);
  private readonly identity = inject(IdentityApi);
  private readonly toasts = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly household = signal<Household | null>(null);
  protected readonly members = signal<Member[]>([]);
  protected readonly invitation = signal<Invitation | null>(null);
  protected readonly pending = signal<PendingInvitation[]>([]);
  protected readonly busy = signal(false);
  protected readonly dateTime = dateTime;

  protected settings = { name: '', defaultCurrency: '', timezone: '' };
  protected inviteEmail = '';
  protected inviteRole: WorkspaceRole = 'MEMBER';
  protected newName = '';
  protected newCurrency = 'GBP';
  protected newTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/London';
  protected inviteInput = '';

  /** Loads the dropdown lists and, if the current workspace is a household, the household. */
  async ngOnInit(): Promise<void> {
    await this.run(async () => {
      await this.ref.load();
      if (!this.ref.timezones().some((z) => z.id === this.newTimezone)) {
        this.newTimezone = 'Europe/London';
      }
      if (this.ws.current()?.type === 'HOUSEHOLD') {
        await this.load();
      }
    });
  }

  /** PATCH /households/{id}. */
  protected async saveSettings(): Promise<void> {
    await this.run(async () => {
      const h = await this.identity.updateHousehold(this.id, this.settings);
      this.apply(h);
      await this.ws.reloadWorkspaces();
      this.toasts.success('Household saved; new transactions default to ' + h.defaultCurrency + '.');
    });
  }

  /** PATCH /households/{id}/members/{userId}. */
  protected async changeRole(member: Member, role: WorkspaceRole): Promise<void> {
    if (role === 'OWNER' && !confirm(`Hand ownership to ${this.ws.name(member.userId)}? You will become an admin.`)) {
      await this.load();
      return;
    }
    await this.run(async () => {
      await this.identity.changeRole(this.id, member.userId, role);
      await this.ws.reloadWorkspaces();
      await this.load();
      this.toasts.success('Role changed.');
    });
  }

  /** DELETE /households/{id}/members/{userId}; removing yourself means leaving. */
  protected async remove(member: Member, self: boolean): Promise<void> {
    const question = self ? 'Leave this household?' : `Remove ${this.ws.name(member.userId)} from the household?`;
    if (!confirm(question)) {
      return;
    }
    await this.run(async () => {
      await this.identity.removeMember(this.id, member.userId);
      if (self) {
        await this.ws.reloadWorkspaces();
        await this.ws.select(this.ws.workspaces()[0].id);
        this.household.set(null);
        this.toasts.success('You left the household.');
      } else {
        await this.load();
        await this.ws.reloadMembers();
        this.toasts.success('Member removed.');
      }
    });
  }

  /** POST /households/{id}/invitations and shows the one-time link. */
  protected async invite(): Promise<void> {
    await this.run(async () => {
      this.invitation.set(await this.identity.invite(this.id, { email: this.inviteEmail, role: this.inviteRole }));
      this.inviteEmail = '';
      await this.loadPending();
    });
  }

  /** POST /households, then switches to the new household. */
  protected async create(): Promise<void> {
    await this.run(async () => {
      const h = await this.identity.createHousehold({
        name: this.newName, defaultCurrency: this.newCurrency, timezone: this.newTimezone,
      });
      await this.ws.reloadWorkspaces();
      await this.ws.select(h.id);
      this.newName = '';
      await this.load();
      this.toasts.success(`Household "${h.name}" created.`);
    });
  }

  /** DELETE /households/{id}/invitations/{invitationId}: the link stops working at once. */
  protected async revoke(inv: PendingInvitation): Promise<void> {
    if (!confirm(`Revoke the invitation for ${inv.email}? The link will stop working.`)) {
      return;
    }
    await this.run(async () => {
      await this.identity.revokeInvitation(this.id, inv.id);
      if (this.invitation()?.id === inv.id) {
        this.invitation.set(null);
      }
      await this.loadPending();
      this.toasts.success('Invitation revoked.');
    });
  }

  /** Opens the accept page for a pasted link or token. */
  protected openInvite(): void {
    const token = this.inviteInput.trim().split('/invitations/').pop()!.split(/[?#]/)[0];
    void this.router.navigate(['/invitations', token]);
  }

  /** The link the invitee opens: this app's /invitations/{token}. */
  protected inviteLink(inv: Invitation): string {
    return `${window.location.origin}/invitations/${inv.token}`;
  }

  /** Copies text to the clipboard. */
  protected async copy(text: string): Promise<void> {
    await navigator.clipboard.writeText(text);
    this.toasts.success('Copied.');
  }

  /** The current household's ID. */
  private get id(): string {
    return this.ws.current()!.id;
  }

  /** GET /households/{id} and its members. */
  private async load(): Promise<void> {
    const [h, m] = await Promise.all([this.identity.household(this.id), this.identity.members(this.id)]);
    this.apply(h);
    this.members.set(m.items);
    await this.loadPending();
  }

  /** GET /households/{id}/invitations, for owners and admins only. */
  private async loadPending(): Promise<void> {
    this.pending.set(this.ws.isAdmin() ? (await this.identity.invitations(this.id)).items : []);
  }

  /** Shows a household and fills the settings form. */
  private apply(h: Household): void {
    this.household.set(h);
    this.settings = { name: h.name, defaultCurrency: h.defaultCurrency, timezone: h.timezone };
  }

  /** Runs an action with the buttons disabled, showing any API error. */
  private async run(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    try {
      await action();
    } catch (error) {
      this.toasts.error(error);
    } finally {
      this.busy.set(false);
    }
  }
}

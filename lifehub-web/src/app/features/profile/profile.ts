import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { IdentityApi } from '../../core/api/identity.api';
import { NotificationApi, NotificationPreference } from '../../core/api/notification.api';
import { AuthService } from '../../core/auth/auth.service';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { ToastService } from '../../shared/toast.service';

/** The user's profile (identity-workspace) and notification settings (notification). */
@Component({
  selector: 'lh-profile',
  imports: [FormsModule],
  template: `
    <header class="page-head"><div class="grow"><h1>Profile</h1><p class="subtitle">Your details and how LifeHub reaches you.</p></div></header>
    <section class="card">
      <h2>Your details</h2>
      <form class="form" (ngSubmit)="save()">
        <label>Display name <input name="displayName" [(ngModel)]="displayName" maxlength="100" /></label>
        <label>Email <input name="email" type="email" [(ngModel)]="email" maxlength="254" /></label>
        @if (ws.me()?.email) {
          <p class="verify-row">
            @if (ws.me()?.emailVerified) {
              <span class="tag success">Verified</span> Notification emails go to this address.
            } @else {
              <span class="tag danger">Not verified</span> No email is sent until you confirm it is yours.
              <button type="button" class="button ghost" (click)="sendVerification()" [disabled]="busy()">Send verification link</button>
            }
          </p>
        }
        <p class="hint">Leave a field empty to clear it. Changing your email means verifying the new one.</p>
        <button class="button" [disabled]="busy()">Save</button>
      </form>
      <details>
        <summary>Technical details</summary>
        <dl class="kv">
          <dt>LifeHub user ID</dt><dd><code>{{ ws.me()?.id }}</code></dd>
          <dt>Logto subject</dt><dd><code>{{ auth.claims()?.sub }}</code></dd>
          <dt>Joined</dt><dd>{{ ws.me()?.createdAt }}</dd>
        </dl>
      </details>
    </section>

    <section class="card">
      <h2>Notification settings</h2>
      @if (preferences().length) {
        <table class="table">
          <thead><tr><th>Type</th><th>In the app</th><th>Email</th></tr></thead>
          <tbody>
            @for (p of preferences(); track p.type) {
              <tr>
                <td>{{ label(p.type) }}</td>
                <td><input type="checkbox" [(ngModel)]="p.inApp" [name]="'in-' + p.type" /></td>
                <td><input type="checkbox" [(ngModel)]="p.email" [name]="'em-' + p.type" /></td>
              </tr>
            }
          </tbody>
        </table>
        <button class="button" (click)="savePreferences()" [disabled]="busy()">Save settings</button>
      } @else {
        <p class="muted">Loading…</p>
      }
    </section>
  `,
})
export class Profile implements OnInit {
  protected readonly ws = inject(WorkspaceService);
  protected readonly auth = inject(AuthService);
  private readonly identity = inject(IdentityApi);
  private readonly notifications = inject(NotificationApi);
  private readonly toasts = inject(ToastService);

  protected displayName = '';
  protected email = '';
  protected readonly preferences = signal<NotificationPreference[]>([]);
  protected readonly busy = signal(false);

  /** Fills the form and loads the notification settings. */
  async ngOnInit(): Promise<void> {
    this.displayName = this.ws.me()?.displayName ?? '';
    this.email = this.ws.me()?.email ?? '';
    try {
      this.preferences.set(await this.notifications.preferences());
    } catch (error) {
      this.toasts.error(error);
    }
  }

  /** PATCH /me with both fields. */
  protected async save(): Promise<void> {
    await this.run(async () => {
      this.ws.me.set(await this.identity.updateMe({ displayName: this.displayName, email: this.email }));
      await this.ws.reloadMembers();
      this.toasts.success('Profile saved.');
    });
  }

  /** POST /me/email-verification: mails a one-time link to the saved email. */
  protected async sendVerification(): Promise<void> {
    await this.run(async () => {
      await this.identity.requestEmailVerification();
      this.toasts.success(`Link sent to ${this.ws.me()?.email}. Open it while signed in to LifeHub.`);
    });
  }

  /** PUT /me/notification-preferences with every type. */
  protected async savePreferences(): Promise<void> {
    await this.run(async () => {
      this.preferences.set(await this.notifications.updatePreferences(this.preferences()));
      this.toasts.success('Notification settings saved.');
    });
  }

  /** "SHARED_EXPENSE" as "Shared expense". */
  protected label(type: string): string {
    const words = type.toLowerCase().split('_').join(' ');
    return words.charAt(0).toUpperCase() + words.slice(1);
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

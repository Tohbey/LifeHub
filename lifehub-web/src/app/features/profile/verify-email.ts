import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { describeError } from '../../core/api/errors';
import { IdentityApi } from '../../core/api/identity.api';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { Icon } from '../../shared/icon';

/** The page the verification email links to: uses the token as soon as it opens. */
@Component({
  selector: 'lh-verify-email',
  imports: [RouterLink, Icon],
  template: `
    <header class="page-head"><div class="grow"><h1>Confirm your email</h1></div></header>
    <section class="card center-card">
      @if (state() === 'working') {
        <div class="loading"><span class="spinner"></span> Confirming…</div>
      } @else if (state() === 'done') {
        <div class="empty-icon tone-success"><lh-icon name="check" [size]="28" /></div>
        <h2>Email confirmed</h2>
        <p>Notification emails will go to <strong>{{ ws.me()?.email }}</strong>.</p>
        <a class="button" routerLink="/profile">Back to your profile</a>
      } @else {
        <div class="empty-icon danger"><lh-icon name="alert" [size]="28" /></div>
        <h2>That link did not work</h2>
        <p>{{ error() }}</p>
        <a class="button" routerLink="/profile">Send a new link</a>
      }
    </section>
  `,
})
export class VerifyEmail implements OnInit {
  protected readonly ws = inject(WorkspaceService);
  private readonly identity = inject(IdentityApi);

  /** The token from /verify-email/:token. */
  readonly token = input.required<string>();
  protected readonly state = signal<'working' | 'done' | 'failed'>('working');
  protected readonly error = signal('');

  /** POST /me/email-verification/{token}. */
  async ngOnInit(): Promise<void> {
    try {
      this.ws.me.set(await this.identity.confirmEmail(this.token()));
      this.state.set('done');
    } catch (error) {
      this.error.set(describeError(error));
      this.state.set('failed');
    }
  }
}

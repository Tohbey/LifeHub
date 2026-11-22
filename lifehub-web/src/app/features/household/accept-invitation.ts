import { Component, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';

import { IdentityApi } from '../../core/api/identity.api';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { ToastService } from '../../shared/toast.service';

/** The page an invite link opens: accepting joins the household and switches to it. */
@Component({
  selector: 'lh-accept-invitation',
  template: `
    <header class="page-head"><div class="grow"><h1>Join a household</h1><p class="subtitle">Accept the invitation to start sharing.</p></div></header>
    <section class="card">
      <p>You have been invited to join a household on LifeHub. Accepting adds you as a member with the role you
        were invited with. The link works once.</p>
      <button class="button" (click)="accept()" [disabled]="busy()">Accept invitation</button>
    </section>
  `,
})
export class AcceptInvitation {
  private readonly identity = inject(IdentityApi);
  private readonly ws = inject(WorkspaceService);
  private readonly toasts = inject(ToastService);
  private readonly router = inject(Router);

  /** The token from /invitations/:token. */
  readonly token = input.required<string>();
  protected readonly busy = signal(false);

  /** POST /invitations/{token}/accept, then switches to the household. */
  protected async accept(): Promise<void> {
    this.busy.set(true);
    try {
      const workspace = await this.identity.acceptInvitation(this.token());
      await this.ws.reloadWorkspaces();
      await this.ws.select(workspace.id);
      this.toasts.success(`You joined ${workspace.name}.`);
      await this.router.navigate(['/household']);
    } catch (error) {
      this.toasts.error(error);
    } finally {
      this.busy.set(false);
    }
  }
}

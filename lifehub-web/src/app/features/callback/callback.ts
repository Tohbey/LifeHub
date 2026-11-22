import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

/** Logto redirects here after sign-in; exchanges the code for tokens and continues to the requested page. */
@Component({
  selector: 'lh-callback',
  template: `
    <main class="center">
      @if (error()) {
        <div class="card error-card">
          <h2>Sign-in failed</h2>
          <p>{{ error() }}</p>
          <button class="button" (click)="retry()">Try again</button>
        </div>
      } @else {
        <p class="muted">Signing you in…</p>
      }
    </main>
  `,
})
export class Callback implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly error = signal<string | null>(null);

  /** Completes the sign-in. */
  async ngOnInit(): Promise<void> {
    try {
      const returnTo = await this.auth.handleCallback();
      await this.router.navigateByUrl(returnTo, { replaceUrl: true });
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : String(error));
    }
  }

  /** Starts sign-in again. */
  protected retry(): void {
    void this.auth.signIn('/');
  }
}

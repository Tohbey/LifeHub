import { Component, DestroyRef, inject, signal } from '@angular/core';
import { SwUpdate } from '@angular/service-worker';

/**
 * Tells the user when a new version of the app has been downloaded, and reloads into it on request. Besides the
 * check the service worker makes on every load, it checks every 30 minutes and whenever the tab comes back into
 * view, so someone who keeps LifeHub open for days still hears about updates.
 */
@Component({
  selector: 'lh-update-banner',
  template: `
    @if (available()) {
      <div class="update-banner">
        A new version of LifeHub is ready.
        <button class="button" (click)="reload()">Reload</button>
      </div>
    }
  `,
})
export class UpdateBanner {
  /** How often an open app asks whether a new version was deployed. */
  static readonly CHECK_EVERY_MS = 30 * 60 * 1000;

  private readonly updates = inject(SwUpdate);
  protected readonly available = signal(false);

  /** Listens for a downloaded update and checks regularly; the service worker is off in development. */
  constructor() {
    if (!this.updates.isEnabled) {
      return;
    }
    this.updates.versionUpdates.subscribe((event) => {
      if (event.type === 'VERSION_READY') {
        this.available.set(true);
      }
    });
    const timer = setInterval(() => this.check(), UpdateBanner.CHECK_EVERY_MS);
    const onVisible = () => document.visibilityState === 'visible' && this.check();
    document.addEventListener('visibilitychange', onVisible);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(timer);
      document.removeEventListener('visibilitychange', onVisible);
    });
  }

  /** Asks the service worker to look for a new version; a failed check (offline) is retried next time. */
  private check(): void {
    this.updates.checkForUpdate().catch(() => undefined);
  }

  /** Switches to the new version. */
  protected reload(): void {
    document.location.reload();
  }
}

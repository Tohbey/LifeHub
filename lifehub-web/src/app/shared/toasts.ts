import { Component, inject } from '@angular/core';

import { Icon } from './icon';
import { ToastService } from './toast.service';

/** Renders the toast messages, each with an icon for its kind. */
@Component({
  selector: 'lh-toasts',
  imports: [Icon],
  template: `
    <div class="toasts" aria-live="polite">
      @for (toast of toasts.toasts(); track toast.id) {
        <div [class]="'toast toast-' + toast.kind" role="status">
          <span class="toast-icon"><lh-icon [name]="icons[toast.kind]" [size]="16" /></span>
          <span>{{ toast.text }}</span>
          <button class="icon-button" (click)="toasts.dismiss(toast.id)" aria-label="Dismiss"><lh-icon name="close" [size]="16" /></button>
        </div>
      }
    </div>
  `,
})
export class Toasts {
  protected readonly toasts = inject(ToastService);
  protected readonly icons = { success: 'check', error: 'alert', info: 'bell' } as const;
}

import { Injectable, signal } from '@angular/core';

import { describeError } from '../core/api/errors';

export interface Toast { id: number; kind: 'success' | 'error' | 'info'; text: string; }

/** Short messages in the corner: what worked, and exactly what the API said when it did not. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<Toast[]>([]);

  /** Shows a success message. */
  success(text: string): void {
    this.push('success', text, 4000);
  }

  /** Shows an info message. */
  info(text: string): void {
    this.push('info', text, 6000);
  }

  /** Shows a failed call: status, title, detail and field errors. Stays longer, so it can be read. */
  error(error: unknown): void {
    this.push('error', describeError(error), 12000);
  }

  /** Removes a message. */
  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  /** Adds a message that disappears after {@code ms}. */
  private push(kind: Toast['kind'], text: string, ms: number): void {
    const toast = { id: this.nextId++, kind, text };
    this.toasts.update((list) => [...list.slice(-4), toast]);
    setTimeout(() => this.dismiss(toast.id), ms);
  }
}

import { WritableSignal } from '@angular/core';

import { ToastService } from './toast.service';

/** Runs an action with {@code busy} set while it runs, and shows any API error as a toast. */
export async function runWithToast(busy: WritableSignal<boolean>, toasts: ToastService,
    action: () => Promise<void>): Promise<void> {
  busy.set(true);
  try {
    await action();
  } catch (error) {
    toasts.error(error);
  } finally {
    busy.set(false);
  }
}

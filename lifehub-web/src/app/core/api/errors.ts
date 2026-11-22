import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from './models';

/** Shown for any failed call while the device has no network. */
export const OFFLINE_MESSAGE = 'You are offline. LifeHub will load your data again once you reconnect.';

/** Turns any failed call into one readable line: status, the API's detail, and field errors if any. */
export function describeError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return error instanceof Error ? error.message : String(error);
  }
  // Offline, the browser fails the call (status 0) or the service worker answers 504 itself.
  if ((error.status === 0 || error.status === 504) && !navigator.onLine) {
    return OFFLINE_MESSAGE;
  }
  if (error.status === 0) {
    return 'Could not reach LifeHub. Is lifehub-app running?';
  }
  const problem = (typeof error.error === 'object' ? error.error : null) as ProblemDetail | null;
  const parts = [`${error.status} ${problem?.title ?? error.statusText}`];
  if (problem?.detail) {
    parts.push(problem.detail);
  }
  if (problem?.errors) {
    parts.push(Object.entries(problem.errors).map(([field, message]) => `${field}: ${message}`).join('; '));
  }
  if (error.status === 503 && !problem?.detail) {
    parts.push('The service is not registered or not running.');
  }
  return parts.join(' – ');
}

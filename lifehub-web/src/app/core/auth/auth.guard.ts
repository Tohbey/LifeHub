import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { AuthService } from './auth.service';

/** Sends signed-out users to Logto, and brings them back to the page they asked for. */
export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  if (auth.authenticated()) {
    return true;
  }
  await auth.signIn(state.url);
  return false;
};

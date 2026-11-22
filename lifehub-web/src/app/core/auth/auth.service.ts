import { Injectable, signal } from '@angular/core';
import LogtoClient, { IdTokenClaims } from '@logto/browser';

import { AppConfig } from '../config/app-config';

const RETURN_TO_KEY = 'lifehub.returnTo';

/**
 * Signs the user in with Logto (authorization code + PKCE) and hands out access tokens for the LifeHub API.
 * Tokens are kept by the Logto SDK; the app never stores them itself.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private client?: LogtoClient;
  private apiResource = '';

  /** True once the user has a Logto session. */
  readonly authenticated = signal(false);
  /** Claims of the ID token: name, email, sub. */
  readonly claims = signal<IdTokenClaims | null>(null);

  /** Creates the Logto client and restores an existing session. */
  async init(config: AppConfig): Promise<void> {
    this.apiResource = config.apiResource;
    this.client = new LogtoClient({
      endpoint: config.logtoEndpoint,
      appId: config.logtoAppId,
      resources: [config.apiResource],
      scopes: ['email', 'profile'],
    });
    await this.refreshState();
  }

  /** Redirects to Logto's sign-in page; after signing in the user comes back to {@code returnTo}. */
  async signIn(returnTo = '/'): Promise<void> {
    sessionStorage.setItem(RETURN_TO_KEY, returnTo);
    await this.logto.signIn(`${window.location.origin}/callback`);
  }

  /** Finishes sign-in on /callback and returns the path the user wanted. */
  async handleCallback(): Promise<string> {
    await this.logto.handleSignInCallback(window.location.href);
    await this.refreshState();
    const returnTo = sessionStorage.getItem(RETURN_TO_KEY) ?? '/';
    sessionStorage.removeItem(RETURN_TO_KEY);
    // Only ever return to a path inside this app, never to another site.
    return returnTo.startsWith('/') && !returnTo.startsWith('//') ? returnTo : '/';
  }

  /** Ends the Logto session and comes back to the app's start page. */
  async signOut(): Promise<void> {
    await this.logto.signOut(window.location.origin);
  }

  /** An access token for the LifeHub API, refreshed by the SDK when it is about to expire. */
  accessToken(): Promise<string> {
    return this.logto.getAccessToken(this.apiResource);
  }

  /** Re-reads whether there is a session and who is signed in. */
  private async refreshState(): Promise<void> {
    const authenticated = await this.logto.isAuthenticated();
    this.authenticated.set(authenticated);
    this.claims.set(authenticated ? await this.logto.getIdTokenClaims() : null);
  }

  /** The Logto client, once {@link init} has run. */
  private get logto(): LogtoClient {
    if (!this.client) {
      throw new Error('AuthService.init() has not run');
    }
    return this.client;
  }
}

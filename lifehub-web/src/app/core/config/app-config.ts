import { Injectable } from '@angular/core';

/** Settings read at startup from /app-config.json, so one build works on every machine. */
export interface AppConfig {
  /** Logto's base URL, without /oidc, for example https://auth.homelab.dev. */
  logtoEndpoint: string;
  /** The ID of the Logto single-page application. */
  logtoAppId: string;
  /** The API resource indicator; must equal lifehub.security.audience on the server. */
  apiResource: string;
}

/** Holds the loaded configuration for the rest of the app. */
@Injectable({ providedIn: 'root' })
export class AppConfigStore {
  private value?: AppConfig;

  /** Downloads /app-config.json. Fails loudly if it is missing or incomplete. */
  async load(): Promise<AppConfig> {
    const response = await fetch('/app-config.json', { cache: 'no-store' });
    if (!response.ok) {
      throw new Error(`Could not load /app-config.json (${response.status})`);
    }
    const config = (await response.json()) as AppConfig;
    for (const key of ['logtoEndpoint', 'logtoAppId', 'apiResource'] as const) {
      if (!config[key]) {
        throw new Error(`/app-config.json is missing "${key}"`);
      }
    }
    this.value = config;
    return config;
  }

  /** The loaded configuration. */
  get config(): AppConfig {
    if (!this.value) {
      throw new Error('App configuration has not been loaded yet');
    }
    return this.value;
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface CurrencyOption { code: string; name: string; symbol: string; common: boolean; }
export interface TimezoneOption { id: string; offset: string; common: boolean; }
export interface ReferenceData { currencies: CurrencyOption[]; timezones: TimezoneOption[]; }

/**
 * Currencies and timezones for dropdowns, served by lifehub-app (GET /api/v1/reference-data). Loaded once and
 * shared by every page; the preferred ones come first.
 */
@Injectable({ providedIn: 'root' })
export class ReferenceDataStore {
  private readonly http = inject(HttpClient);
  private loading?: Promise<void>;

  readonly data = signal<ReferenceData>({ currencies: [], timezones: [] });
  readonly currencies = computed(() => this.data().currencies);
  readonly timezones = computed(() => this.data().timezones);

  /** Loads the lists the first time it is called; later calls reuse them. A failure is retried on the next call. */
  load(): Promise<void> {
    this.loading ??= firstValueFrom(this.http.get<ReferenceData>('/api/v1/reference-data'))
      .then((data) => this.data.set(data))
      .catch((error) => {
        this.loading = undefined;
        throw error;
      });
    return this.loading;
  }

  /** "GBP · British Pound (£)" for a dropdown. */
  currencyLabel(c: CurrencyOption): string {
    return c.symbol && c.symbol !== c.code ? `${c.code} · ${c.name} (${c.symbol})` : `${c.code} · ${c.name}`;
  }

  /** "(UTC+01:00) Europe/Paris" for a dropdown. */
  timezoneLabel(z: TimezoneOption): string {
    return `(UTC${z.offset}) ${z.id.replaceAll('_', ' ')}`;
  }
}

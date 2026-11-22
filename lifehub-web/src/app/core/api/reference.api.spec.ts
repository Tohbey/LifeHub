import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ReferenceDataStore } from './reference.api';

describe('ReferenceDataStore', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }));

  it('loads the lists once and labels them for dropdowns', async () => {
    const store = TestBed.inject(ReferenceDataStore);
    const http = TestBed.inject(HttpTestingController);

    const first = store.load();
    const second = store.load();
    http.expectOne('/api/v1/reference-data').flush({
      currencies: [{ code: 'GBP', name: 'British Pound', symbol: '£', common: true }, { code: 'NGN', name: 'Naira', symbol: 'NGN', common: true }],
      timezones: [{ id: 'America/New_York', offset: '-05:00', common: true }],
    });
    await Promise.all([first, second]);
    http.verify();

    expect(store.currencyLabel(store.currencies()[0])).toBe('GBP · British Pound (£)');
    expect(store.currencyLabel(store.currencies()[1])).toBe('NGN · Naira');
    expect(store.timezoneLabel(store.timezones()[0])).toBe('(UTC-05:00) America/New York');
  });

  it('tries again after a failed load', async () => {
    const store = TestBed.inject(ReferenceDataStore);
    const http = TestBed.inject(HttpTestingController);

    const failed = store.load();
    http.expectOne('/api/v1/reference-data').flush('down', { status: 503, statusText: 'Service Unavailable' });
    await expect(failed).rejects.toBeTruthy();

    const retry = store.load();
    http.expectOne('/api/v1/reference-data').flush({ currencies: [], timezones: [] });
    await retry;
  });
});

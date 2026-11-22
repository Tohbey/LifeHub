import { HttpErrorResponse } from '@angular/common/http';

import { describeError } from './errors';

describe('describeError', () => {
  it('shows the status, title, detail and field errors of a ProblemDetail', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { title: 'Bad Request', detail: 'Some fields are invalid.', errors: { amount: 'must be greater than 0' } },
    });
    expect(describeError(error)).toBe('400 Bad Request – Some fields are invalid. – amount: must be greater than 0');
  });

  it('explains a network failure', () => {
    expect(describeError(new HttpErrorResponse({ status: 0 }))).toBe('Could not reach LifeHub. Is lifehub-app running?');
  });

  it('says so when the device is offline', () => {
    const online = vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);
    expect(describeError(new HttpErrorResponse({ status: 504, statusText: 'Gateway Timeout' })))
      .toBe('You are offline. LifeHub will load your data again once you reconnect.');
    online.mockRestore();
  });

  it('explains a 503 without a body as a service that is not running', () => {
    expect(describeError(new HttpErrorResponse({ status: 503, statusText: 'Service Unavailable' })))
      .toBe('503 Service Unavailable – The service is not registered or not running.');
  });

  it('falls back to the message of other errors', () => {
    expect(describeError(new Error('boom'))).toBe('boom');
    expect(describeError('plain')).toBe('plain');
  });
});

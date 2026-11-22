import { dateTime, money, toInstant, toLocalInput } from './format';

describe('format helpers', () => {
  it('formats money with the currency symbol', () => {
    expect(money(120, 'GBP')).toContain('120.00');
    expect(money(120, 'GBP')).toContain('£');
  });

  it('falls back to a plain amount for an unknown currency code', () => {
    expect(money(5, 'NOT-A-CODE')).toBe('5 NOT-A-CODE');
  });

  it('round-trips a local date-time input through an instant', () => {
    const local = '2026-11-20T18:30';
    expect(toLocalInput(toInstant(local)!)).toBe(local);
    expect(toInstant('')).toBeUndefined();
  });

  it('shows a dash for a missing date', () => {
    expect(dateTime(null)).toBe('–');
    expect(dateTime('2026-11-20T18:30:00Z')).not.toBe('–');
  });
});

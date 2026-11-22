/** Formatting helpers shared by the feature pages. */

/** "£120.00" in the user's locale. */
export function money(amount: number | string, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(Number(amount));
  } catch {
    return `${amount} ${currency}`;
  }
}

/** A readable local date and time, or "–". */
export function dateTime(iso: string | null | undefined): string {
  return iso ? new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '–';
}

/** Turns a <input type="datetime-local"> value into an ISO instant, or undefined when empty. */
export function toInstant(local: string): string | undefined {
  return local ? new Date(local).toISOString() : undefined;
}

/** The value for a <input type="datetime-local"> from an instant. */
export function toLocalInput(iso: string | Date): string {
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

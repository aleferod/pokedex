const oneDecimal = new Intl.NumberFormat('en', { maximumFractionDigits: 1 });

/** "6.9 kg": the API's kilograms, with at most one decimal. */
export function formatKilograms(value: number): string {
  return `${oneDecimal.format(value)} kg`;
}

/** "0.7 m": the API's metres, with at most one decimal. */
export function formatMetres(value: number): string {
  return `${oneDecimal.format(value)} m`;
}

const dateTime = new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' });

/** "Oct 15, 2026, 9:00 AM" in the reader's time zone, for audit dates. */
export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso));
}

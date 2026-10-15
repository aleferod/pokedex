import type { FieldError } from 'react-hook-form';

/**
 * Every message of a field error: with `criteriaMode: 'all'` a field can break several rules at
 * once (React Hook Form keeps them in `types`), and server errors may carry several too.
 */
export function fieldMessages(error: FieldError | undefined): string[] {
  if (error === undefined) {
    return [];
  }
  const messages = new Set<string>();
  for (const value of Object.values(error.types ?? {})) {
    for (const message of Array.isArray(value) ? value : [value]) {
      if (typeof message === 'string' && message.length > 0) {
        messages.add(message);
      }
    }
  }
  if (messages.size === 0 && error.message) {
    messages.add(error.message);
  }
  return [...messages];
}

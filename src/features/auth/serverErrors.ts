import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';

import { ApiError } from '@/shared/api';

const UNEXPECTED = 'Something went wrong. Please try again.';

/**
 * Puts an API failure where the user will see it: field errors (400, and the field a 409 names)
 * on their inputs, anything else in the form-level alert. Returns that alert's text, if any.
 */
export function showServerErrors<T extends FieldValues>(
  error: unknown,
  fields: readonly Path<T>[],
  setError: UseFormSetError<T>,
): string | null {
  if (!(error instanceof ApiError)) {
    return UNEXPECTED;
  }
  let placed = false;
  for (const field of fields) {
    const messages = error.fieldErrors[field];
    if (messages !== undefined && messages.length > 0) {
      setError(field, {
        type: 'server',
        message: messages[0] ?? '',
        types: Object.fromEntries(
          messages.map((message, index) => [`server${String(index)}`, message]),
        ),
      });
      placed = true;
    }
  }
  return placed ? null : error.message;
}

import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';

import { ApiError } from '@/shared/api';

const UNEXPECTED = 'Something went wrong. Please try again.';

/**
 * Where an API field error belongs in a form: a list of fields named as in the API, or a function
 * from the API's field name (such as `localizedNames.pt-BR`) to the form field, `undefined` when
 * the form has no place for it.
 */
export type ServerFieldMap<T extends FieldValues> =
  readonly Path<T>[] | ((apiField: string) => Path<T> | undefined);

/**
 * Puts an API failure where the user will see it: field errors (400, and the field a 409 names)
 * on their inputs, anything else in the form-level alert. Returns that alert's text, if any: the
 * API's message when an error could not be placed on a field.
 */
export function showServerErrors<T extends FieldValues>(
  error: unknown,
  fields: ServerFieldMap<T>,
  setError: UseFormSetError<T>,
): string | null {
  if (!(error instanceof ApiError)) {
    return UNEXPECTED;
  }
  const fieldFor =
    typeof fields === 'function'
      ? fields
      : (apiField: string) => fields.find((field) => field === apiField);
  const placed = new Map<Path<T>, string[]>();
  let unplaced = false;
  for (const [apiField, messages] of Object.entries(error.fieldErrors)) {
    const field = fieldFor(apiField);
    if (field === undefined) {
      unplaced = true;
    } else {
      placed.set(field, [...(placed.get(field) ?? []), ...messages]);
    }
  }
  for (const [field, messages] of placed) {
    setError(field, {
      type: 'server',
      message: messages[0] ?? '',
      types: Object.fromEntries(
        messages.map((message, index) => [`server${String(index)}`, message]),
      ),
    });
  }
  return placed.size > 0 && !unplaced ? null : error.message;
}

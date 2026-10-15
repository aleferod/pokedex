import { ApiError } from '@/shared/api';

const UNAVAILABLE = 'PokeAPI is temporarily unavailable. Please try again in a moment.';
const UNEXPECTED = 'Something went wrong. Please try again.';

/** What to tell the user about a failed query: 503 means PokeAPI is down, otherwise the API's message. */
export function queryErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.status === 503 ? UNAVAILABLE : error.message;
  }
  return UNEXPECTED;
}

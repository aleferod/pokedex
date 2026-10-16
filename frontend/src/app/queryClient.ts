import { QueryClient } from '@tanstack/react-query';

import { ApiError } from '@/shared/api';

/**
 * A 4xx answer will not change by asking again (bad input, not found, not allowed, conflict);
 * anything else (5xx, network, unexpected) gets one more attempt.
 */
export function shouldRetry(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return false;
  }
  return failureCount < 1;
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: shouldRetry, staleTime: 60_000, refetchOnWindowFocus: false },
    },
  });
}

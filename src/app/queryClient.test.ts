import { describe, expect, it } from 'vitest';

import { ApiError } from '@/shared/api';

import { createQueryClient, shouldRetry } from './queryClient';

function apiError(status: number): ApiError {
  return new ApiError('failed', status);
}

describe('query retry policy', () => {
  it.each([400, 401, 403, 404, 409])('does not retry a %i', (status) => {
    expect(shouldRetry(0, apiError(status))).toBe(false);
  });

  it('retries a server error or a network failure once', () => {
    expect(shouldRetry(0, apiError(503))).toBe(true);
    expect(shouldRetry(0, apiError(0))).toBe(true);
    expect(shouldRetry(1, apiError(503))).toBe(false);
  });

  it('retries an unexpected error once', () => {
    expect(shouldRetry(0, new Error('boom'))).toBe(true);
    expect(shouldRetry(1, new Error('boom'))).toBe(false);
  });

  it('keeps data fresh for a minute and does not refetch on focus', () => {
    const defaults = createQueryClient().getDefaultOptions().queries;

    expect(defaults?.staleTime).toBe(60_000);
    expect(defaults?.refetchOnWindowFocus).toBe(false);
    expect(defaults?.retry).toBe(shouldRetry);
  });
});

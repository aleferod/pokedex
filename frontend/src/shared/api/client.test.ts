import { delay, http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { server } from '@/test/server';

import { ApiError } from './ApiError';
import { apiRequest, configureApiClient } from './client';

const PROBLEM_TYPE = 'https://github.com/aleferod/pokedex/blob/main/backend/docs/problems.md#';

function problem(status: number, slug: string, detail: string, errors: object[] = []) {
  return HttpResponse.json(
    {
      type: PROBLEM_TYPE + slug,
      title: 'Title of ' + slug,
      status,
      detail,
      instance: '/api/v1/x',
      timestamp: '2026-10-15T12:00:00Z',
      errors,
    },
    {
      status,
      headers: { 'Content-Type': 'application/problem+json', 'X-Correlation-Id': 'corr-123' },
    },
  );
}

async function rejection(promise: Promise<unknown>): Promise<unknown> {
  try {
    await promise;
  } catch (error) {
    return error;
  }
  throw new Error('expected the request to fail');
}

describe('apiRequest', () => {
  afterEach(() => {
    configureApiClient({});
  });

  it('calls the API base URL on the same origin and accepts JSON', async () => {
    let seen: Request | undefined;
    server.use(
      http.get('/api/v1/pokemon', ({ request }) => {
        seen = request;
        return HttpResponse.json({ content: [] });
      }),
    );

    const body = await apiRequest<{ content: unknown[] }>('/pokemon?page=0&size=20');

    expect(body).toEqual({ content: [] });
    expect(new URL(seen?.url ?? '').pathname).toBe('/api/v1/pokemon');
    expect(new URL(seen?.url ?? '').search).toBe('?page=0&size=20');
    expect(seen?.headers.get('Accept')).toBe('application/json');
    expect(seen?.headers.has('Authorization')).toBe(false);
  });

  it('sends a JSON body', async () => {
    let received: unknown;
    let contentType: string | null = null;
    server.use(
      http.post('/api/v1/local-pokemon', async ({ request }) => {
        contentType = request.headers.get('Content-Type');
        received = await request.json();
        return HttpResponse.json({ id: 'abc' }, { status: 201 });
      }),
    );

    await apiRequest('/local-pokemon', { method: 'POST', body: { pokeapiId: 1 } });

    expect(contentType).toBe('application/json');
    expect(received).toEqual({ pokeapiId: 1 });
  });

  it('resolves without a value when the API answers 204', async () => {
    let deleted = false;
    server.use(
      http.delete('/api/v1/local-pokemon/1', () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await expect(apiRequest('/local-pokemon/1', { method: 'DELETE' })).resolves.toBeUndefined();
    expect(deleted).toBe(true);
  });

  it('turns a validation problem into an ApiError with field errors grouped by field', async () => {
    server.use(
      http.post('/api/v1/local-pokemon', () =>
        problem(400, 'bad-request', 'The request has invalid fields.', [
          { field: 'title', message: 'is required' },
          { field: 'title', message: 'is too long' },
          { field: 'region', message: 'must be at most 50 characters' },
        ]),
      ),
    );

    const error = await rejection(apiRequest('/local-pokemon', { method: 'POST', body: {} }));

    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(400);
    expect(apiError.type).toBe('bad-request');
    expect(apiError.title).toBe('Title of bad-request');
    expect(apiError.message).toBe('The request has invalid fields.');
    expect(apiError.fieldErrors).toEqual({
      title: ['is required', 'is too long'],
      region: ['must be at most 50 characters'],
    });
    expect(apiError.correlationId).toBe('corr-123');
    expect(apiError.isNetworkError).toBe(false);
  });

  it('keeps the problem type and detail of a conflict', async () => {
    server.use(
      http.patch('/api/v1/local-pokemon/1', () =>
        problem(409, 'conflict', 'Changed by someone else; reload it and retry.'),
      ),
    );

    const error = (await rejection(
      apiRequest('/local-pokemon/1', { method: 'PATCH', body: {} }),
    )) as ApiError;

    expect(error.status).toBe(409);
    expect(error.type).toBe('conflict');
    expect(error.detail).toBe('Changed by someone else; reload it and retry.');
    expect(error.message).toBe('Changed by someone else; reload it and retry.');
  });

  it('still produces an ApiError when the error body is not a problem document', async () => {
    server.use(
      http.get(
        '/api/v1/pokemon',
        () =>
          new HttpResponse('<html>Bad Gateway</html>', {
            status: 502,
            headers: { 'Content-Type': 'text/html' },
          }),
      ),
    );

    const error = (await rejection(apiRequest('/pokemon'))) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(502);
    expect(error.type).toBeUndefined();
    expect(error.fieldErrors).toEqual({});
    expect(error.message).toBe('The server answered with an error (502). Please try again.');
  });

  it('marks a failed connection as a network error', async () => {
    server.use(http.get('/api/v1/pokemon', () => HttpResponse.error()));

    const error = (await rejection(apiRequest('/pokemon'))) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(0);
    expect(error.isNetworkError).toBe(true);
    expect(error.message).toBe('Cannot reach the server. Check your connection and try again.');
  });

  it('lets a cancelled request reject with the abort error itself', async () => {
    server.use(
      http.get('/api/v1/pokemon', async () => {
        await delay('infinite');
        return HttpResponse.json({});
      }),
    );
    const controller = new AbortController();

    const pending = apiRequest('/pokemon', { signal: controller.signal });
    controller.abort();
    const error = await rejection(pending);

    expect(error).not.toBeInstanceOf(ApiError);
    expect((error as Error).name).toBe('AbortError');
  });

  it('sends the bearer token and reports a 401 when auth is configured', async () => {
    let authorization: string | null = null;
    server.use(
      http.get('/api/v1/auth/me', ({ request }) => {
        authorization = request.headers.get('Authorization');
        return problem(401, 'unauthorized', 'The token is invalid or has expired.');
      }),
    );
    const onUnauthorized = vi.fn();
    configureApiClient({ getToken: () => 'token-123', onUnauthorized });

    const error = (await rejection(apiRequest('/auth/me'))) as ApiError;

    expect(authorization).toBe('Bearer token-123');
    expect(error.status).toBe(401);
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });
});

import { ApiError } from './ApiError';

/** Options of one API call. */
export interface ApiRequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

/** Hooks the auth feature plugs in: where the token comes from, and what a 401 triggers. */
export interface ApiClientConfig {
  getToken?: () => string | null;
  onUnauthorized?: () => void;
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

let config: ApiClientConfig = {};

export function configureApiClient(next: ApiClientConfig): void {
  config = next;
}

/**
 * Calls pokedex-api: `path` is relative to the API base URL (`/pokemon?page=0`). Resolves the JSON
 * body (nothing for 204 or an empty body); rejects with an {@link ApiError}, except that a cancelled
 * request rejects with its original `AbortError` so query libraries can ignore it.
 */
export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const headers = new Headers({ Accept: 'application/json', ...options.headers });
  const token = config.getToken?.();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  const init: RequestInit = { method: options.method ?? 'GET', headers };
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json');
    init.body = JSON.stringify(options.body);
  }
  if (options.signal) {
    init.signal = options.signal;
  }

  let response: Response;
  try {
    response = await fetch(new URL(`${BASE_URL}${path}`, window.location.origin), init);
  } catch (error) {
    if (isAbort(error)) {
      throw error;
    }
    throw ApiError.network(error);
  }

  if (!response.ok) {
    const error = await ApiError.fromResponse(response);
    if (response.status === 401) {
      config.onUnauthorized?.();
    }
    throw error;
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  return (text.length > 0 ? JSON.parse(text) : undefined) as T;
}

function isAbort(error: unknown): boolean {
  return error instanceof Error && error.name === 'AbortError';
}

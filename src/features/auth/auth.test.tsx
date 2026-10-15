import { useQuery } from '@tanstack/react-query';
import { act, screen, waitFor } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { apiRequest } from '@/shared/api';
import { renderRoute } from '@/test/renderRoute';
import { server } from '@/test/server';
import { testSession } from '@/test/sessions';

import { ProtectedRoute } from './ProtectedRoute';
import { SESSION_KEY, saveSession } from './session';

/** A protected page that loads data with the session's token. */
function MyPokedexPage() {
  const { data } = useQuery({
    queryKey: ['local-pokemon'],
    queryFn: ({ signal }) => apiRequest<{ totalElements: number }>('/local-pokemon', { signal }),
  });
  return <p>{data ? `${String(data.totalElements)} records` : 'Loading records'}</p>;
}

const PROTECTED = [
  { Component: ProtectedRoute, children: [{ path: 'my-pokedex', Component: MyPokedexPage }] },
];

function localPokemonAnswers(status: number, seen?: { authorization: string | null }) {
  server.use(
    http.get('/api/v1/local-pokemon', ({ request }) => {
      if (seen) {
        seen.authorization = request.headers.get('Authorization');
      }
      return status === 200
        ? HttpResponse.json({ totalElements: 2 })
        : HttpResponse.json(
            {
              type: 'x#unauthorized',
              title: 'Unauthorized',
              status,
              detail: 'Expired.',
              errors: [],
            },
            { status, headers: { 'Content-Type': 'application/problem+json' } },
          );
    }),
  );
}

describe('session and protected routes', () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.useRealTimers();
  });

  it('sends a visitor from a protected page to sign-in, remembering where they were', async () => {
    const { router } = renderRoute('/my-pokedex?tag=starter', PROTECTED);

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/login');
    });
    expect(router.state.location.search).toBe('?next=%2Fmy-pokedex%3Ftag%3Dstarter');
  });

  it('restores the session after a reload and sends its token', async () => {
    saveSession(testSession());
    const seen = { authorization: null as string | null };
    localPokemonAnswers(200, seen);

    renderRoute('/my-pokedex', PROTECTED);

    expect(await screen.findByText('2 records')).toBeInTheDocument();
    expect(seen.authorization).toBe('Bearer test.jwt.token');
    expect(screen.getByText('Signed in as trainer')).toBeInTheDocument();
  });

  it('ends the session on a 401 and returns to sign-in with a way back', async () => {
    saveSession(testSession());
    localPokemonAnswers(401);

    const { router } = renderRoute('/my-pokedex', PROTECTED);

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/login');
    });
    expect(router.state.location.search).toBe('?next=%2Fmy-pokedex');
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('signs out by itself when the token expires', () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'Date'] });
    vi.setSystemTime(new Date('2026-10-15T12:00:00Z'));
    saveSession(testSession({}, '2026-10-15T12:01:00Z'));

    renderRoute('/');
    expect(screen.getByText('Signed in as trainer')).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(61_000);
    });

    expect(screen.queryByText('Signed in as trainer')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument();
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('signs out on request: clears the session and cached data, and goes home', async () => {
    const user = userEvent.setup();
    saveSession(testSession());
    localPokemonAnswers(200);
    const { router, queryClient } = renderRoute('/my-pokedex', PROTECTED);
    await screen.findByText('2 records');

    await user.click(screen.getByRole('button', { name: 'Sign out' }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/');
    });
    expect(screen.getByRole('link', { name: 'Register' })).toHaveAttribute('href', '/register');
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });

  it('offers sign-in and registration to visitors', async () => {
    renderRoute('/');

    expect(await screen.findByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/login');
    expect(screen.getByRole('link', { name: 'Register' })).toHaveAttribute('href', '/register');
    expect(screen.queryByRole('button', { name: 'Sign out' })).not.toBeInTheDocument();
  });

  it('marks an administrator in the header', async () => {
    saveSession(testSession({ username: 'admin', role: 'ADMIN' }));

    renderRoute('/');

    expect(await screen.findByText('Signed in as admin')).toBeInTheDocument();
    expect(screen.getByText('Admin')).toBeInTheDocument();
  });
});

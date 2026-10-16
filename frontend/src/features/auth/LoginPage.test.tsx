import { useQuery } from '@tanstack/react-query';
import { screen, waitFor } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';

import { apiRequest } from '@/shared/api';
import { problemResponse, signInSucceeds } from '@/test/authApi';
import { renderRoute } from '@/test/renderRoute';
import { server } from '@/test/server';
import { testSession } from '@/test/sessions';

import { ProtectedRoute } from './ProtectedRoute';
import { SESSION_KEY, saveSession } from './session';

function MyPokedexPage() {
  const { data } = useQuery({
    queryKey: ['local-pokemon'],
    queryFn: ({ signal }) => apiRequest<{ totalElements: number }>('/local-pokemon', { signal }),
  });
  return <p>My Pokédex page{data ? `: ${String(data.totalElements)}` : ''}</p>;
}

const PROTECTED = [
  { Component: ProtectedRoute, children: [{ path: 'my-pokedex', Component: MyPokedexPage }] },
];

async function fillAndSubmit(username: string, password: string) {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText('Username'), username);
  await user.type(screen.getByLabelText('Password'), password);
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
  return user;
}

describe('sign-in page', () => {
  afterEach(() => {
    sessionStorage.clear();
  });

  it('signs in and returns to the page the user came from', async () => {
    const seen: { body?: unknown } = {};
    signInSucceeds(testSession(), seen);
    server.use(http.get('/api/v1/local-pokemon', () => HttpResponse.json({ totalElements: 10 })));
    const { router } = renderRoute('/login?next=%2Fmy-pokedex', PROTECTED);

    await fillAndSubmit('trainer', 'Trainer@123');

    expect(await screen.findByText('My Pokédex page: 10')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/my-pokedex');
    expect(screen.getByText('Signed in as trainer')).toBeInTheDocument();
    expect(seen.body).toEqual({ username: 'trainer', password: 'Trainer@123' });
    expect(sessionStorage.getItem(SESSION_KEY)).not.toBeNull();
  });

  it('says the credentials are wrong without saying which part, and stays', async () => {
    server.use(
      http.post('/api/v1/auth/login', () =>
        problemResponse(401, 'unauthorized', 'Invalid username or password.'),
      ),
    );
    const { router } = renderRoute('/login');

    await fillAndSubmit('trainer', 'wrong');

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password.');
    expect(router.state.location.pathname).toBe('/login');
    expect(screen.queryByText(/session has expired/)).not.toBeInTheDocument();
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('explains a network failure', async () => {
    server.use(http.post('/api/v1/auth/login', () => HttpResponse.error()));
    renderRoute('/login');

    await fillAndSubmit('trainer', 'Trainer@123');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Cannot reach the server. Check your connection and try again.',
    );
  });

  it('shows the server field errors next to the fields', async () => {
    server.use(
      http.post('/api/v1/auth/login', () =>
        problemResponse(400, 'bad-request', 'The request has invalid fields.', [
          { field: 'username', message: 'size must be between 0 and 100' },
        ]),
      ),
    );
    renderRoute('/login');

    await fillAndSubmit('trainer', 'Trainer@123');

    const username = screen.getByLabelText('Username');
    await waitFor(() => {
      expect(username).toHaveAttribute('aria-invalid', 'true');
    });
    expect(username).toHaveAccessibleDescription('Size must be between 0 and 100');
  });

  it('marks empty fields as required and focuses the first one, sending nothing', async () => {
    const user = userEvent.setup();
    renderRoute('/login');

    await user.click(await screen.findByRole('button', { name: 'Sign in' }));

    const username = screen.getByLabelText('Username');
    await waitFor(() => {
      expect(username).toHaveFocus();
    });
    expect(username).toHaveAttribute('aria-invalid', 'true');
    expect(username).toHaveAccessibleDescription('Is required');
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Is required');
  });

  it('shows that it is working while the request runs', async () => {
    server.use(
      http.post('/api/v1/auth/login', async () => {
        await delay('infinite');
        return HttpResponse.json({});
      }),
    );
    renderRoute('/login');

    await fillAndSubmit('trainer', 'Trainer@123');

    const button = await screen.findByRole('button', { name: 'Signing in…' });
    expect(button).toBeDisabled();
  });

  it('tells a user whose session expired why they are here', async () => {
    saveSession(testSession());
    server.use(
      http.get('/api/v1/local-pokemon', () => problemResponse(401, 'unauthorized', 'Expired.')),
    );

    const { router } = renderRoute('/my-pokedex', PROTECTED);

    expect(
      await screen.findByText('Your session has expired. Please sign in again.'),
    ).toBeInTheDocument();
    expect(router.state.location.search).toBe('?next=%2Fmy-pokedex');
  });

  it('ignores a next parameter pointing to another site', async () => {
    signInSucceeds();
    const { router } = renderRoute('/login?next=%2F%2Fevil.example');

    await fillAndSubmit('trainer', 'Trainer@123');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/');
    });
  });

  it('sends a signed-in user straight on', async () => {
    saveSession(testSession());
    server.use(http.get('/api/v1/local-pokemon', () => HttpResponse.json({ totalElements: 3 })));

    const { router } = renderRoute('/login?next=%2Fmy-pokedex', PROTECTED);

    expect(await screen.findByText('My Pokédex page: 3')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/my-pokedex');
  });

  it('labels the page and offers registration', async () => {
    renderRoute('/login?next=%2Fmy-pokedex');

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Create an account' })).toHaveAttribute(
      'href',
      '/register?next=%2Fmy-pokedex',
    );
    await waitFor(() => {
      expect(document.title).toBe('Sign in · Pokédex');
    });
  });
});

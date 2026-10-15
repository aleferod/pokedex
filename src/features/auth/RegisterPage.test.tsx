import { screen, waitFor } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';

import { problemResponse, signInSucceeds } from '@/test/authApi';
import { renderRoute } from '@/test/renderRoute';
import { server } from '@/test/server';
import { testSession } from '@/test/sessions';

async function fill(values: { username?: string; email?: string; password?: string }) {
  const user = userEvent.setup();
  if (values.username !== undefined) {
    await user.type(await screen.findByLabelText('Username'), values.username);
  }
  if (values.email !== undefined) {
    await user.type(screen.getByLabelText('Email'), values.email);
  }
  if (values.password !== undefined) {
    await user.type(screen.getByLabelText('Password'), values.password);
  }
  return user;
}

const MISTY = { username: ' Misty ', email: 'Misty@Cerulean.City', password: 'Starmie@121' };

describe('registration page', () => {
  afterEach(() => {
    sessionStorage.clear();
  });

  it('lists every unmet password rule as soon as the field is left, sending nothing', async () => {
    renderRoute('/register');
    const user = await fill({ password: 'abc' });

    await user.tab();

    const password = screen.getByLabelText('Password');
    await waitFor(() => {
      expect(password).toHaveAttribute('aria-invalid', 'true');
    });
    const errors = screen.getByTestId('password-errors');
    expect(errors).toHaveTextContent('Must be 8 to 72 characters');
    expect(errors).toHaveTextContent('Must contain an upper-case letter');
    expect(errors).toHaveTextContent('Must contain a digit');
    expect(errors).toHaveTextContent(
      'Must contain a character that is neither a letter nor a digit',
    );
    expect(errors).not.toHaveTextContent('lower-case');
  });

  it('explains the username and email rules in the backend words', async () => {
    renderRoute('/register');
    const user = await fill({ username: 'a!', email: 'not-an-email' });

    await user.tab();

    await waitFor(() => {
      expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true');
    });
    expect(screen.getByLabelText('Username')).toHaveAccessibleDescription(
      expect.stringContaining("Must be 3 to 30 characters: letters, digits, '.', '_' or '-'"),
    );
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription(
      'Must be a valid email address',
    );
  });

  it('creates the account, signs it in and goes home', async () => {
    let registered: unknown;
    server.use(
      http.post('/api/v1/auth/register', async ({ request }) => {
        registered = await request.json();
        return HttpResponse.json(testSession({ username: 'misty' }).user, { status: 201 });
      }),
    );
    const signIn: { body?: unknown } = {};
    signInSucceeds(testSession({ username: 'misty' }), signIn);
    const { router } = renderRoute('/register');
    const user = await fill(MISTY);

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('Signed in as misty')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/');
    expect(registered).toEqual({
      username: 'misty',
      email: 'misty@cerulean.city',
      password: 'Starmie@121',
    });
    expect(signIn.body).toEqual({ username: 'misty', password: 'Starmie@121' });
  });

  it.each([
    ['username', 'Username'],
    ['email', 'Email'],
  ])('marks a taken %s on its field', async (field, label) => {
    server.use(
      http.post('/api/v1/auth/register', () =>
        problemResponse(409, 'conflict', `An account with this ${field} already exists.`, [
          { field, message: 'is already taken' },
        ]),
      ),
    );
    renderRoute('/register');
    const user = await fill(MISTY);

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    const input = screen.getByLabelText(label);
    await waitFor(() => {
      expect(input).toHaveAttribute('aria-invalid', 'true');
    });
    expect(input).toHaveAccessibleDescription(expect.stringContaining('Is already taken'));
  });

  it('shows every server message for a field', async () => {
    server.use(
      http.post('/api/v1/auth/register', () =>
        problemResponse(400, 'bad-request', 'The password is too weak.', [
          { field: 'password', message: 'must contain a digit' },
          { field: 'password', message: 'must be 8 to 72 characters' },
        ]),
      ),
    );
    renderRoute('/register');
    const user = await fill(MISTY);

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    const errors = await screen.findByTestId('password-errors');
    expect(errors).toHaveTextContent('Must contain a digit');
    expect(errors).toHaveTextContent('Must be 8 to 72 characters');
  });

  it('explains a server failure', async () => {
    server.use(http.post('/api/v1/auth/register', () => new HttpResponse('oops', { status: 500 })));
    renderRoute('/register');
    const user = await fill(MISTY);

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The server answered with an error (500). Please try again.',
    );
  });

  it('labels the page and offers sign-in instead', async () => {
    renderRoute('/register');

    expect(await screen.findByRole('heading', { name: 'Create an account' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Sign in instead' })).toHaveAttribute('href', '/login');
    await waitFor(() => {
      expect(document.title).toBe('Register · Pokédex');
    });
  });
});

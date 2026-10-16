import { afterEach, describe, expect, it } from 'vitest';

import { clearSession, loadSession, saveSession, SESSION_KEY } from './session';
import type { Session } from './types';

const NOW = new Date('2026-10-15T12:00:00Z');

const SESSION: Session = {
  token: 'header.payload.signature',
  expiresAt: '2026-10-15T13:00:00Z',
  user: {
    id: '6f1c2c1e-1111-4a4a-9b9b-123456789abc',
    username: 'trainer',
    email: 'trainer@pokedex.example',
    role: 'USER',
    createdAt: '2026-10-15T09:00:00Z',
  },
};

describe('stored session', () => {
  afterEach(() => {
    sessionStorage.clear();
  });

  it('reads back what was saved', () => {
    saveSession(SESSION);

    expect(loadSession(NOW)).toEqual(SESSION);
  });

  it('discards an expired session', () => {
    saveSession({ ...SESSION, expiresAt: '2026-10-15T12:00:00Z' });
    expect(sessionStorage.getItem(SESSION_KEY)).not.toBeNull();

    expect(loadSession(NOW)).toBeNull();
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it.each([
    ['not JSON', '{oops'],
    ['wrong shape', JSON.stringify({ token: 42 })],
    ['unknown role', JSON.stringify({ ...SESSION, user: { ...SESSION.user, role: 'ROOT' } })],
    ['bad expiry', JSON.stringify({ ...SESSION, expiresAt: 'tomorrow' })],
  ])('discards a stored value that is %s', (_label, raw) => {
    sessionStorage.setItem(SESSION_KEY, raw);

    expect(loadSession(NOW)).toBeNull();
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('clears the session', () => {
    saveSession(SESSION);
    expect(sessionStorage.getItem(SESSION_KEY)).not.toBeNull();

    clearSession();

    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    expect(loadSession(NOW)).toBeNull();
  });

  it('uses sessionStorage, not localStorage', () => {
    saveSession(SESSION);

    expect(sessionStorage.getItem(SESSION_KEY)).not.toBeNull();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
  });
});

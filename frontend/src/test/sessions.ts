import type { Session } from '@/features/auth';

/** A session for tests, valid for an hour from now unless `expiresAt` says otherwise. */
export function testSession(overrides: Partial<Session['user']> = {}, expiresAt?: string): Session {
  return {
    token: 'test.jwt.token',
    expiresAt: expiresAt ?? new Date(Date.now() + 3_600_000).toISOString(),
    user: {
      id: '6f1c2c1e-1111-4a4a-9b9b-123456789abc',
      username: 'trainer',
      email: 'trainer@pokedex.example',
      role: 'USER',
      createdAt: '2026-10-15T09:00:00Z',
      ...overrides,
    },
  };
}

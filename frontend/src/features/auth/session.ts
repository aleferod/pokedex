import { z } from 'zod';

import type { Session } from './types';

/** Where the session lives: this tab only, gone when it closes (see the design, D1). */
export const SESSION_KEY = 'pokedex.session';

const storedSession = z.object({
  token: z.string().min(1),
  expiresAt: z.iso.datetime({ offset: true }),
  user: z.object({
    id: z.string().min(1),
    username: z.string().min(1),
    email: z.string(),
    role: z.enum(['USER', 'ADMIN']),
    createdAt: z.string(),
  }),
});

export function saveSession(session: Session): void {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

/** The stored session if it is well-formed and not expired; anything else is removed. */
export function loadSession(now: Date = new Date()): Session | null {
  const raw = sessionStorage.getItem(SESSION_KEY);
  if (raw === null) {
    return null;
  }
  const parsed = storedSession.safeParse(parseJson(raw));
  if (!parsed.success || Date.parse(parsed.data.expiresAt) <= now.getTime()) {
    clearSession();
    return null;
  }
  return parsed.data;
}

export function clearSession(): void {
  sessionStorage.removeItem(SESSION_KEY);
}

function parseJson(raw: string): unknown {
  try {
    return JSON.parse(raw) as unknown;
  } catch {
    return undefined;
  }
}

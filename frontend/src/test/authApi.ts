import { http, HttpResponse } from 'msw';

import type { Session } from '@/features/auth';

import { server } from './server';
import { testSession } from './sessions';

const PROBLEM = 'https://github.com/aleferod/pokedex-api/blob/main/docs/problems.md#';

/** An RFC 7807 answer as pokedex-api sends it. */
export function problemResponse(
  status: number,
  slug: string,
  detail: string,
  errors: { field: string; message: string }[] = [],
) {
  return HttpResponse.json(
    { type: PROBLEM + slug, title: slug, status, detail, instance: '/api/v1', errors },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );
}

/** POST /auth/login answering with a token for `session` (recording the body). */
export function signInSucceeds(session: Session = testSession(), seen?: { body?: unknown }) {
  server.use(
    http.post('/api/v1/auth/login', async ({ request }) => {
      if (seen) {
        seen.body = await request.json();
      }
      return HttpResponse.json({
        accessToken: session.token,
        tokenType: 'Bearer',
        expiresAt: session.expiresAt,
        user: session.user,
      });
    }),
  );
}

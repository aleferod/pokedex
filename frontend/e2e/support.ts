import { expect, type APIRequestContext, type Page } from '@playwright/test';

/** The demo accounts pokedex-api seeds with SEED_DEMO_DATA=true (public by design). */
export const TRAINER = { username: 'trainer', password: 'Trainer@123' } as const;
export const ADMIN = { username: 'admin', password: 'Admin@123' } as const;

export interface Account {
  username: string;
  password: string;
}

/** Fills the sign-in form on the current page (or opens it) and waits for the session. */
export async function signIn(page: Page, account: Account, open = true): Promise<void> {
  if (open) {
    await page.goto('/login');
  }
  await page.getByLabel('Username').fill(account.username);
  await page.getByLabel('Password').fill(account.password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByText(`Signed in as ${account.username}`)).toBeVisible();
}

/** A second client: a token for the API, as another tab or user would have. */
export async function apiToken(request: APIRequestContext, account: Account): Promise<string> {
  const response = await request.post('/api/v1/auth/login', { data: account });
  expect(response.ok()).toBe(true);
  const body = (await response.json()) as { accessToken: string };
  return body.accessToken;
}

/** That second client saves a proprietary change first, with the record's current ETag. */
export async function saveElsewhere(
  request: APIRequestContext,
  token: string,
  id: string,
  patch: Record<string, unknown>,
): Promise<void> {
  const auth = { Authorization: `Bearer ${token}` };
  const current = await request.get(`/api/v1/local-pokemon/${id}`, { headers: auth });
  const etag = current.headers().etag;
  expect(etag).toBeDefined();
  const saved = await request.patch(`/api/v1/local-pokemon/${id}`, {
    headers: { ...auth, 'If-Match': etag ?? '' },
    data: patch,
  });
  expect(saved.status()).toBe(200);
}

import { expect, test } from '@playwright/test';

import { signIn } from './support';

test('registers, signs out of a protected page and signs in again', async ({ page }) => {
  const suffix = `${Date.now().toString(36)}${String(Math.floor(Math.random() * 1000))}`;
  const account = { username: `e2e-${suffix}`, password: 'E2e-Pass-1234!' };

  await page.goto('/register');
  await page.getByLabel('Username').fill(account.username);
  await page.getByLabel('Email').fill(`${account.username}@example.com`);
  await page.getByLabel('Password').fill(account.password);
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.getByText(`Signed in as ${account.username}`)).toBeVisible();

  await page.getByRole('link', { name: 'My Pokédex' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'My Pokédex' })).toBeVisible();
  await page.getByRole('button', { name: 'Sign out' }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole('link', { name: 'Sign in' })).toBeVisible();

  await page.goto('/login');
  await page.getByLabel('Username').fill(account.username);
  await page.getByLabel('Password').fill('Wrong-Pass-1234!');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('alert')).toContainText('Invalid username or password.');

  await signIn(page, account, false);
});

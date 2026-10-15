import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';

import { ADMIN, signIn } from './support';

const WCAG_A_AA = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

/**
 * Runs axe on the page as it is now. Serious and critical violations fail the test, listed with
 * their targets and help; minor and moderate ones are attached to the report.
 */
async function expectAccessible(page: Page, view: string): Promise<void> {
  const results = await new AxeBuilder({ page }).withTags(WCAG_A_AA).analyze();
  const describe = (violation: (typeof results.violations)[number]) =>
    `${violation.id} (${String(violation.impact)}) on ${violation.nodes
      .map((node) => node.target.join(' '))
      .join(', ')}: ${violation.help} ${violation.helpUrl}`;
  const blocking = results.violations.filter(
    (violation) => violation.impact === 'serious' || violation.impact === 'critical',
  );
  const advisory = results.violations.filter((violation) => !blocking.includes(violation));
  if (advisory.length > 0) {
    test
      .info()
      .annotations.push({ type: `axe: ${view}`, description: advisory.map(describe).join('\n') });
  }
  expect(blocking.map(describe), `serious or critical violations on ${view}`).toEqual([]);
}

test.describe('accessibility (axe, WCAG 2.1 A and AA)', () => {
  test('public pages', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expectAccessible(page, 'home');

    await page.goto('/pokemon');
    await expect(page.getByRole('heading', { name: 'Bulbasaur' })).toBeVisible();
    await expectAccessible(page, 'catalog');

    await page.goto('/pokemon/133');
    await expect(page.getByRole('heading', { level: 1, name: 'Eevee' })).toBeVisible();
    await expectAccessible(page, 'a Pokémon with a branching chain');

    await page.goto('/pokemon/missingno');
    await expect(page.getByRole('heading', { level: 1, name: 'Pokémon not found' })).toBeVisible();
    await expectAccessible(page, 'Pokémon not found');

    await page.goto('/no-such-page');
    await expect(page.getByRole('heading', { level: 1, name: 'Page not found' })).toBeVisible();
    await expectAccessible(page, 'page not found');
  });

  test('forms showing their errors', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByLabel('Username')).toHaveAttribute('aria-invalid', 'true');
    await expectAccessible(page, 'sign-in with errors');

    await page.goto('/register');
    await page.getByLabel('Password').fill('abc');
    await page.getByRole('button', { name: 'Create account' }).click();
    await expect(page.getByLabel('Password')).toHaveAttribute('aria-invalid', 'true');
    await expectAccessible(page, 'registration with errors');
  });

  test('the local Pokédex', async ({ page }) => {
    await signIn(page, ADMIN);

    await page.goto('/local');
    // Not a record count: local.e2e.ts syncs and deletes Eevee in another worker meanwhile.
    await expect(page.getByRole('link', { name: 'Pikachu' })).toBeVisible();
    await expectAccessible(page, 'My Pokédex');

    await page.getByRole('link', { name: 'Pikachu' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Pikachu' })).toBeVisible();
    await expectAccessible(page, 'a record');

    await page.getByRole('button', { name: 'Delete' }).click();
    await expect(page.getByRole('group', { name: 'Delete Pikachu?' })).toBeVisible();
    await expectAccessible(page, 'a record with the delete confirmation');
    await page.getByRole('button', { name: 'Cancel' }).click();

    await page.getByRole('link', { name: 'Edit' }).click();
    await page.getByLabel('Region').fill('a'.repeat(51));
    await page.getByLabel('Tags').fill('Bad Tag!');
    await page.getByRole('button', { name: 'Add a localized name' }).click();
    await page.getByRole('button', { name: 'Save changes' }).click();
    await expect(page.getByLabel('Region')).toHaveAttribute('aria-invalid', 'true');
    await expectAccessible(page, 'the edit form with errors');

    await page.goto('/local/new');
    await page.getByRole('button', { name: 'Add to my Pokédex' }).click();
    await expect(page.getByLabel('Pokédex number')).toHaveAttribute('aria-invalid', 'true');
    await expectAccessible(page, 'the create form with errors');
  });
});

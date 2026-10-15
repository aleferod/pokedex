import { expect, test, type Page } from '@playwright/test';

import { apiToken, saveElsewhere, signIn, TRAINER } from './support';

/*
 * Regenerates docs/screenshots/ from the real stack with the demo data:
 *   scripts/e2e.sh --screenshots
 * Not part of the normal run (see playwright.config.ts).
 */

const DIR = 'docs/screenshots';
const DESKTOP = { width: 1280, height: 860 };
const PHONE = { width: 360, height: 780 };

/** Waits for the page's images (sprites, artwork) before taking the picture. */
async function shoot(page: Page, name: string, fullPage = false): Promise<void> {
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: `${DIR}/${name}.png`, fullPage });
}

test.describe('@screenshots', () => {
  test.describe.configure({ mode: 'serial' });

  test('public pages', async ({ page }) => {
    await page.setViewportSize(DESKTOP);
    await page.goto('/pokemon');
    await expect(page.getByRole('heading', { name: 'Bulbasaur' })).toBeVisible();
    await shoot(page, 'catalog');

    await page.goto('/pokemon/133');
    await expect(page.getByRole('heading', { level: 1, name: 'Eevee' })).toBeVisible();
    await shoot(page, 'detail', true);

    await page.setViewportSize(PHONE);
    await page.goto('/pokemon');
    await expect(page.getByRole('heading', { name: 'Bulbasaur' })).toBeVisible();
    await shoot(page, 'catalog-phone');
    await page.goto('/pokemon/25');
    await expect(page.getByRole('heading', { level: 1, name: 'Pikachu' })).toBeVisible();
    await shoot(page, 'detail-phone');
  });

  test('the local Pokédex', async ({ page, request }) => {
    await page.setViewportSize(DESKTOP);
    await signIn(page, TRAINER);
    await page.goto('/local');
    await expect(page.getByText('10 records')).toBeVisible();
    await shoot(page, 'my-pokedex');

    await page.getByRole('link', { name: 'Pikachu' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Pikachu' })).toBeVisible();
    const id = /\/local\/([0-9a-f-]{36})/.exec(page.url())?.[1] ?? '';
    await shoot(page, 'record', true);

    await page.getByRole('link', { name: 'Edit' }).click();
    await expect(page.getByLabel('Region')).toHaveValue('Kanto');
    await saveElsewhere(request, await apiToken(request, TRAINER), id, {
      notes: 'Saved by another client',
    });
    await page.getByLabel('Region').fill('Johto');
    await page.getByRole('button', { name: 'Save changes' }).click();
    await expect(page.getByRole('alert')).toContainText('Someone else changed this record');
    // The alert sits above the form; the page is still scrolled to the Save button.
    await page.evaluate(() => {
      window.scrollTo(0, 0);
    });
    await shoot(page, 'edit-conflict');
  });
});

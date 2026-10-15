import { expect, test, type Page } from '@playwright/test';

import { ADMIN, apiToken, saveElsewhere, signIn, TRAINER } from './support';

// These steps share the seeded records and Eevee, which the first ones sync: one at a time.
test.describe.configure({ mode: 'serial' });

function syncPanel(page: Page) {
  return page.getByRole('region', { name: 'Sync from PokeAPI' });
}

function recordId(page: Page): string {
  const id = /\/local\/([0-9a-f-]{36})/.exec(page.url())?.[1];
  expect(id).toBeDefined();
  return id ?? '';
}

test.describe('the local Pokédex', () => {
  test('sends a visitor to sign in and back, then filters by tag', async ({ page }) => {
    await page.goto('/local?tag=starter');
    await expect(page).toHaveURL(/\/login\?next=%2Flocal%3Ftag%3Dstarter$/);
    await signIn(page, TRAINER, false);

    await expect(page).toHaveURL(/\/local\?tag=starter$/);
    const names = page.getByRole('list', { name: 'Records' }).getByRole('heading');
    await expect(names).toHaveText(['Bulbasaur', 'Charmander', 'Squirtle']);

    await page.getByRole('link', { name: 'Filter by tag fire' }).click();
    await expect(page).toHaveURL(/\/local\?tag=fire$/);
    await expect(names).toHaveText(['Charmander', 'Charmeleon', 'Charizard']);
    await page.getByRole('link', { name: 'Clear filters' }).click();
    await expect(page.getByText('10 records')).toBeVisible();
  });

  test('syncs Pokémon, edits tags in place and re-syncs', async ({ page }) => {
    await signIn(page, TRAINER);
    await page.goto('/local');

    await syncPanel(page).getByLabel('Name or number').fill('25');
    await syncPanel(page).getByRole('button', { name: 'Sync', exact: true }).click();
    await expect(
      page.getByText('Pikachu was re-synced from PokeAPI. Its proprietary fields were kept.'),
    ).toBeVisible();

    await page.goto('/local');
    await syncPanel(page).getByLabel('Name or number').fill('Eevee');
    await syncPanel(page).getByRole('button', { name: 'Sync', exact: true }).click();
    await expect(page.getByText('Eevee was imported from PokeAPI.')).toBeVisible();
    await expect(page.getByRole('heading', { level: 1, name: 'Eevee' })).toBeVisible();

    const own = page.getByRole('region', { name: 'Your data' });
    await own.getByLabel('Add a tag').fill('  Eeveelution ');
    await own.getByRole('button', { name: 'Add', exact: true }).click();
    await expect(own.getByRole('list', { name: 'Tags' })).toContainText('eeveelution');
    await own.getByRole('button', { name: 'Remove tag eeveelution' }).click();
    await expect(own.getByText('No tags yet')).toBeVisible();

    await page.getByRole('button', { name: 'Re-sync from PokeAPI' }).click();
    await expect(
      page
        .getByRole('region', { name: 'From PokeAPI' })
        .getByText('Eevee was re-synced from PokeAPI. Its proprietary fields were kept.'),
    ).toBeVisible();
  });

  test('saves an edit, and recovers when another client saved first', async ({ page, request }) => {
    await signIn(page, TRAINER);
    await page.goto('/local?name=eevee');
    await page.getByRole('link', { name: 'Eevee' }).click();
    const id = recordId(page);

    await page.getByRole('link', { name: 'Edit' }).click();
    await page.getByLabel('Region').fill('Kanto');
    await page.getByRole('button', { name: 'Save changes' }).click();
    await expect(page.getByText('Your changes were saved.')).toBeVisible();
    await expect(page.getByRole('region', { name: 'Your data' })).toContainText('Kanto');

    await page.getByRole('link', { name: 'Edit' }).click();
    await expect(page.getByLabel('Region')).toHaveValue('Kanto');
    await saveElsewhere(request, await apiToken(request, TRAINER), id, {
      notes: 'Saved by another client',
    });
    await page.getByLabel('Notes', { exact: true }).fill('Evolves eight ways');
    await page.getByRole('button', { name: 'Save changes' }).click();

    const conflict = page.getByRole('alert');
    await expect(conflict).toContainText(
      'Someone else changed this record after you opened it, so your changes weren’t saved.',
    );
    await expect(page.getByLabel('Notes', { exact: true })).toHaveValue('Evolves eight ways');
    await conflict.getByRole('button', { name: 'Load the latest version' }).click();
    await expect(page.getByLabel('Notes', { exact: true })).toHaveValue('Saved by another client');

    await page.getByLabel('Notes', { exact: true }).fill('Evolves eight ways');
    await page.getByRole('button', { name: 'Save changes' }).click();
    await expect(page.getByText('Your changes were saved.')).toBeVisible();
    await expect(page.getByRole('region', { name: 'Your data' })).toContainText(
      'Evolves eight ways',
    );
  });

  test('lets only an administrator delete a record', async ({ page }) => {
    await signIn(page, TRAINER);
    await page.goto('/local?name=eevee');
    await page.getByRole('link', { name: 'Eevee' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Eevee' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Delete' })).toHaveCount(0);
    await page.getByRole('button', { name: 'Sign out' }).click();

    await signIn(page, ADMIN);
    await page.goto('/local?name=eevee');
    await page.getByRole('link', { name: 'Eevee' }).click();
    await page.getByRole('button', { name: 'Delete' }).click();
    const confirm = page.getByRole('group', { name: 'Delete Eevee?' });
    await expect(confirm.getByRole('button', { name: 'Cancel' })).toBeFocused();
    await confirm.getByRole('button', { name: 'Delete Eevee' }).click();

    await expect(page).toHaveURL(/\/local$/);
    await expect(page.getByText('Eevee was deleted.')).toBeVisible();
    await expect(page.getByText('10 records')).toBeVisible();
  });
});

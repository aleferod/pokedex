import { expect, test } from '@playwright/test';

test.describe('a visitor', () => {
  test('browses the catalog, a Pokémon and its evolution chain, and comes back', async ({
    page,
  }) => {
    await page.goto('/');
    await page.getByRole('link', { name: 'Catalog', exact: true }).click();

    await expect(page.getByRole('heading', { name: 'Bulbasaur' })).toBeVisible();
    await expect(page.getByRole('navigation', { name: 'Pagination' })).toContainText('Page 1 of');
    await page.getByRole('button', { name: 'Next page' }).click();
    await expect(page).toHaveURL(/\/pokemon\?page=2$/);
    await expect(page.getByRole('heading', { name: 'Pokémon', exact: true })).toBeFocused();

    await page.getByRole('heading', { name: 'Pikachu' }).click();
    await expect(page).toHaveURL(/\/pokemon\/25$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Pikachu' })).toBeFocused();
    await expect(page.getByRole('table', { name: 'Base stats' })).toContainText('Total320');
    const evolution = page.getByRole('region', { name: 'Evolution' });
    await expect(evolution.getByRole('link', { name: 'Pikachu' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    await expect(
      page.getByRole('navigation', { name: 'Main' }).getByRole('link', { name: 'Catalog' }),
    ).toHaveAttribute('aria-current', 'true');

    await evolution.getByRole('link', { name: 'Raichu' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Raichu' })).toBeFocused();
    await page.getByRole('link', { name: 'Back to the catalog' }).click();
    await expect(page).toHaveURL(/\/pokemon\?page=2$/);
  });

  test('searches the catalog by part of a name while typing', async ({ page }) => {
    await page.goto('/pokemon');
    const field = page.getByLabel('Find a Pokémon');

    await field.pressSequentially('pika', { delay: 60 });

    await expect(page).toHaveURL(/\/pokemon\?name=pika$/);
    await expect(page.getByText('17 Pokémon match “pika”')).toBeVisible();
    await expect(field).toBeFocused();
    const results = page.getByRole('list', { name: 'Pokémon' });
    await expect(results.getByRole('heading').first()).toHaveText('Pikachu');

    await results.getByRole('heading', { name: 'Pikachu', exact: true }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Pikachu' })).toBeVisible();
    await page.getByRole('link', { name: 'Back to the catalog' }).click();

    await expect(page).toHaveURL(/\/pokemon\?name=pika$/);
    await expect(page.getByLabel('Find a Pokémon')).toHaveValue('pika');
  });

  test('finds a Pokémon by number and is told when one doesn’t exist', async ({ page }) => {
    await page.goto('/pokemon');
    await page.getByLabel('Find a Pokémon').fill('025');
    await page.getByRole('button', { name: 'Find' }).click();
    await expect(page).toHaveURL(/\/pokemon\/25$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Pikachu' })).toBeVisible();

    await page.goto('/pokemon/missingno');
    await expect(page.getByRole('heading', { level: 1, name: 'Pokémon not found' })).toBeVisible();
    await expect(page.getByText('There is no Pokémon called “missingno”.')).toBeVisible();
  });
});

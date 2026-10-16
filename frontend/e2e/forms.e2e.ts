import { expect, test } from '@playwright/test';

import { signIn, TRAINER } from './support';

test('one click submits right after leaving an invalid field', async ({ page }) => {
  await signIn(page, TRAINER);
  await page.goto('/local/new');
  const writes: string[] = [];
  page.on('request', (request) => {
    if (request.method() !== 'GET' && request.url().includes('/api/v1/local-pokemon')) {
      writes.push(`${request.method()} ${request.url()}`);
    }
  });

  await page.getByLabel('Region').fill('a'.repeat(51));
  await page.getByLabel('Tags').click();
  await expect(page.getByLabel('Region')).toHaveAttribute('aria-invalid', 'true');
  await page.getByLabel('Tags').fill('Bad Tag!');
  // Held like a person's click: leaving Tags shows its error above the button meanwhile.
  await page.getByRole('button', { name: 'Add to my Pokédex' }).click({ delay: 150 });

  await expect(page.getByLabel('Pokédex number')).toBeFocused();
  await expect(page.getByLabel('Tags')).toHaveAttribute('aria-invalid', 'true');
  expect(writes).toEqual([]);
});

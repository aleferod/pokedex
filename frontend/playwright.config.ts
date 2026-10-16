import { defineConfig, devices, type PlaywrightTestConfig } from '@playwright/test';

/*
 * End-to-end tests against the full stack (docker-compose.full.yml). Run them with scripts/e2e.sh,
 * which starts a fresh stack, runs this in Playwright's own image, and removes the stack afterwards.
 * E2E_SCREENSHOTS=1 runs only the @screenshots tests, which regenerate docs/screenshots/.
 */

const screenshots = process.env.E2E_SCREENSHOTS === '1';
const inCi = process.env.CI === 'true';

const selection: Pick<PlaywrightTestConfig, 'grep' | 'grepInvert'> = screenshots
  ? { grep: /@screenshots/ }
  : { grepInvert: /@screenshots/ };

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  ...selection,
  forbidOnly: inCi,
  retries: inCi ? 1 : 0,
  workers: 2,
  timeout: 60_000,
  expect: { timeout: 15_000 },
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8100',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});

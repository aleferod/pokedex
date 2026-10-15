import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest';

import { installConsoleGuard, takeConsoleMessages } from './consoleGuard';
import { server } from './server';

// Unmocked requests are failures, not silent passes.
beforeAll(() => {
  server.listen({ onUnhandledFrame: 'error' });
});
afterAll(() => {
  server.close();
});

// Zero console warnings: any console.error or console.warn (React key and act() warnings,
// failed prop types, library deprecations) fails the test that produced it.
beforeEach(() => {
  installConsoleGuard();
});
afterEach(() => {
  cleanup();
  server.resetHandlers();
  const messages = takeConsoleMessages();
  if (messages.length > 0) {
    throw new Error(`Unexpected console output:\n${messages.join('\n')}`);
  }
});

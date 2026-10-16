import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest';

import { installConsoleGuard, takeConsoleMessages } from './consoleGuard';
import { server } from './server';

// findBy*/waitFor give up after 3 s instead of 1 s: the first render of a file in a cold coverage
// run on a busy machine (CI) can take more than a second. A real failure still fails, later.
configure({ asyncUtilTimeout: 3000 });

// Unmocked requests are failures, not silent passes.
beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
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

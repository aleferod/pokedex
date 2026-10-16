import { vi } from 'vitest';

let messages: string[] = [];

/** Records every console.error and console.warn instead of printing it. */
export function installConsoleGuard(): void {
  messages = [];
  for (const level of ['error', 'warn'] as const) {
    vi.spyOn(console, level).mockImplementation((...args: unknown[]) => {
      messages.push(`console.${level}: ${args.map(String).join(' ')}`);
    });
  }
}

/** Returns and forgets what was recorded so far. */
export function takeConsoleMessages(): string[] {
  const taken = messages;
  messages = [];
  return taken;
}

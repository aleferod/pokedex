import { describe, expect, it } from 'vitest';

import { safeNext } from './safeNext';

describe('safeNext (no open redirects)', () => {
  it.each(['/', '/my-pokedex', '/my-pokedex?tag=starter&page=2', '/pokemon/25#stats'])(
    'keeps the same-app path %j',
    (path) => {
      expect(safeNext(path)).toBe(path);
    },
  );

  it.each([
    null,
    undefined,
    '',
    'my-pokedex',
    '//evil.example',
    '/\\evil.example',
    'https://evil.example',
    'javascript:alert(1)',
    ' /my-pokedex',
  ])('falls back to home for %j', (value) => {
    expect(safeNext(value)).toBe('/');
  });
});

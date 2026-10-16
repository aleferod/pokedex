import { describe, expect, it } from 'vitest';

import { catalogSearch, parseName } from './catalogParams';

describe('the search in the URL', () => {
  it('normalises a name like the API does', () => {
    expect(parseName('Pika')).toBe('pika');
    expect(parseName('  Mr   Mime ')).toBe('mr-mime');
    expect(parseName('porygon2')).toBe('porygon2');
    expect(parseName('a'.repeat(50))).toBe('a'.repeat(50));
  });

  it('ignores a search the API would reject', () => {
    for (const value of [null, '', '   ', 'pi', 'pika!', 'pikächu', 'a'.repeat(51)]) {
      expect(parseName(value)).toBe('');
    }
  });
});

describe('the catalog query string', () => {
  it('writes the page, size and search, leaving defaults out', () => {
    expect(catalogSearch(1, 20)).toBe('');
    expect(catalogSearch(2, 20)).toBe('?page=2');
    expect(catalogSearch(1, 20, 'pika')).toBe('?name=pika');
    expect(catalogSearch(3, 40, 'mr-mime')).toBe('?page=3&size=40&name=mr-mime');
  });
});

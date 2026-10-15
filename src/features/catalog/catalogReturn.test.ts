import { describe, expect, it } from 'vitest';

import { catalogReturnSearch, catalogReturnState } from './catalogReturn';

describe('the way back to the catalog', () => {
  it('carries the catalog page and size in a card link', () => {
    expect(catalogReturnState(3, 40)).toEqual({ catalogSearch: '?page=3&size=40' });
    expect(catalogReturnState(1, 20)).toEqual({ catalogSearch: '' });
  });

  it('reads it back on the detail page', () => {
    expect(catalogReturnSearch({ catalogSearch: '?page=3&size=40' })).toBe('?page=3&size=40');
    expect(catalogReturnSearch(catalogReturnState(5, 20))).toBe('?page=5');
  });

  it('leads to the first catalog page without a usable state', () => {
    expect(catalogReturnSearch(null)).toBe('');
    expect(catalogReturnSearch(undefined)).toBe('');
    expect(catalogReturnSearch('?page=3')).toBe('');
    expect(catalogReturnSearch({})).toBe('');
    expect(catalogReturnSearch({ catalogSearch: 3 })).toBe('');
  });

  it('rebuilds the search instead of trusting it', () => {
    expect(catalogReturnSearch({ catalogSearch: '?size=40&page=3' })).toBe('?page=3&size=40');
    expect(catalogReturnSearch({ catalogSearch: '?page=1&size=20' })).toBe('');
    expect(catalogReturnSearch({ catalogSearch: '?page=abc&size=7&next=//evil.example' })).toBe('');
    expect(catalogReturnSearch({ catalogSearch: '//evil.example' })).toBe('');
  });
});

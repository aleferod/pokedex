import { catalogSearch, parseName, parsePage, parseSize } from './catalogParams';

/** What a card link carries so the detail page can lead back to this catalog view (and search). */
export interface CatalogReturnState {
  catalogSearch: string;
}

export function catalogReturnState(page: number, size: number, name = ''): CatalogReturnState {
  return { catalogSearch: catalogSearch(page, size, name) };
}

/**
 * The catalog search to return to, read from a location's state; `''` means the first page.
 * History state is not trusted: only the page, size and search are read from it, and the query is
 * rebuilt from them.
 */
export function catalogReturnSearch(state: unknown): string {
  if (typeof state !== 'object' || state === null || !('catalogSearch' in state)) {
    return '';
  }
  const search = state.catalogSearch;
  if (typeof search !== 'string' || !search.startsWith('?')) {
    return '';
  }
  const params = new URLSearchParams(search);
  return catalogSearch(
    parsePage(params.get('page')),
    parseSize(params.get('size')),
    parseName(params.get('name')),
  );
}

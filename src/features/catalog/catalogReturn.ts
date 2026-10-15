import { catalogSearch, parsePage, parseSize } from './catalogParams';

/** What a card link carries so the detail page can lead back to this catalog view. */
export interface CatalogReturnState {
  catalogSearch: string;
}

export function catalogReturnState(page: number, size: number): CatalogReturnState {
  return { catalogSearch: catalogSearch(page, size) };
}

/**
 * The catalog search to return to, read from a location's state; `''` means the first page.
 * History state is not trusted: only the page and size are read from it, and the search is
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
  return catalogSearch(parsePage(params.get('page')), parseSize(params.get('size')));
}

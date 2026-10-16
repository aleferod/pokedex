import { apiRequest, type Page } from '@/shared/api';

import type { CatalogEntry } from './types';

/**
 * One catalog page; `page` is 1-based here and 0-based in the API. With a `name`, only the entries
 * whose name contains it (pokedex-api's search).
 */
export function fetchCatalogPage(
  page: number,
  size: number,
  name: string,
  signal?: AbortSignal,
): Promise<Page<CatalogEntry>> {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) });
  if (name !== '') {
    query.set('name', name);
  }
  return apiRequest<Page<CatalogEntry>>(`/pokemon?${query.toString()}`, signal ? { signal } : {});
}

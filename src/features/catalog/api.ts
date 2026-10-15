import { apiRequest, type Page } from '@/shared/api';

import type { CatalogEntry } from './types';

/** One catalog page; `page` is 1-based here and 0-based in the API. */
export function fetchCatalogPage(
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<Page<CatalogEntry>> {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) });
  return apiRequest<Page<CatalogEntry>>(`/pokemon?${query.toString()}`, signal ? { signal } : {});
}

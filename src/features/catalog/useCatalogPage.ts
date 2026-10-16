import { keepPreviousData, queryOptions, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';

import { fetchCatalogPage } from './api';

function catalogQuery(page: number, size: number, name: string) {
  return queryOptions({
    queryKey: ['catalog', page, size, name],
    queryFn: ({ signal }) => fetchCatalogPage(page, size, name, signal),
  });
}

/**
 * One catalog page. While another page loads, the previous one stays on screen
 * (`isPlaceholderData`), and once a page has loaded the next one is fetched ahead of time.
 */
export function useCatalogPage(page: number, size: number, name = '') {
  const queryClient = useQueryClient();
  const query = useQuery({ ...catalogQuery(page, size, name), placeholderData: keepPreviousData });
  const totalPages = query.data?.totalPages ?? 0;
  const loadedThisPage = query.isSuccess && !query.isPlaceholderData;

  useEffect(() => {
    if (loadedThisPage && page < totalPages) {
      // A prefetch is best effort: a failure here must not surface anywhere.
      queryClient.query(catalogQuery(page + 1, size, name)).catch(() => undefined);
    }
  }, [loadedThisPage, page, size, name, totalPages, queryClient]);

  return query;
}

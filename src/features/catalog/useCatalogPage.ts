import { keepPreviousData, queryOptions, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';

import { fetchCatalogPage } from './api';

function catalogQuery(page: number, size: number) {
  return queryOptions({
    queryKey: ['catalog', page, size],
    queryFn: ({ signal }) => fetchCatalogPage(page, size, signal),
  });
}

/**
 * One catalog page. While another page loads, the previous one stays on screen
 * (`isPlaceholderData`), and once a page has loaded the next one is fetched ahead of time.
 */
export function useCatalogPage(page: number, size: number) {
  const queryClient = useQueryClient();
  const query = useQuery({ ...catalogQuery(page, size), placeholderData: keepPreviousData });
  const totalPages = query.data?.totalPages ?? 0;
  const loadedThisPage = query.isSuccess && !query.isPlaceholderData;

  useEffect(() => {
    if (loadedThisPage && page < totalPages) {
      // A prefetch is best effort: a failure here must not surface anywhere.
      queryClient.query(catalogQuery(page + 1, size)).catch(() => undefined);
    }
  }, [loadedThisPage, page, size, totalPages, queryClient]);

  return query;
}

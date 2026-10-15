import { useSearchParams } from 'react-router';

import { catalogSearch, parsePage, parseSize } from './catalogParams';

/** Page and size from the URL, so the view can be shared and survives a reload. */
export function useCatalogParams() {
  const [params, setParams] = useSearchParams();
  const page = parsePage(params.get('page'));
  const size = parseSize(params.get('size'));
  return {
    page,
    size,
    setPage: (next: number) => {
      setParams(new URLSearchParams(catalogSearch(next, size)));
    },
    setSize: (next: number) => {
      setParams(new URLSearchParams(catalogSearch(1, next)));
    },
  };
}

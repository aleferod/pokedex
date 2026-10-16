import { useSearchParams } from 'react-router';

import { catalogSearch, parseName, parsePage, parseSize } from './catalogParams';

/** Page, size and search from the URL, so the view can be shared and survives a reload. */
export function useCatalogParams() {
  const [params, setParams] = useSearchParams();
  const page = parsePage(params.get('page'));
  const size = parseSize(params.get('size'));
  const name = parseName(params.get('name'));
  return {
    page,
    size,
    name,
    setPage: (next: number) => {
      setParams(new URLSearchParams(catalogSearch(next, size, name)));
    },
    setSize: (next: number) => {
      setParams(new URLSearchParams(catalogSearch(1, next, name)));
    },
  };
}

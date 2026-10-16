import { useSearchParams } from 'react-router';

import { DEFAULT_SIZE, parsePage, parseSize } from '@/features/catalog';

/** The list's view, from the URL: 1-based page, size, and the name and tag filters. */
export interface LocalListParams {
  page: number;
  size: number;
  name: string;
  tag: string;
}

/** The query string for a view, leaving defaults and empty filters out (`/local` is page 1 of 20). */
export function localSearch({ page, size, name, tag }: LocalListParams): string {
  const params = new URLSearchParams();
  if (page !== 1) {
    params.set('page', String(page));
  }
  if (size !== DEFAULT_SIZE) {
    params.set('size', String(size));
  }
  if (name !== '') {
    params.set('name', name);
  }
  if (tag !== '') {
    params.set('tag', tag);
  }
  const search = params.toString();
  return search === '' ? '' : `?${search}`;
}

export function useLocalParams() {
  const [searchParams, setSearchParams] = useSearchParams();
  const params: LocalListParams = {
    page: parsePage(searchParams.get('page')),
    size: parseSize(searchParams.get('size')),
    name: (searchParams.get('name') ?? '').trim(),
    tag: (searchParams.get('tag') ?? '').trim(),
  };
  const go = (next: LocalListParams) => {
    setSearchParams(new URLSearchParams(localSearch(next)));
  };
  return {
    params,
    setPage: (page: number) => {
      go({ ...params, page });
    },
    setSize: (size: number) => {
      go({ ...params, page: 1, size });
    },
    /** New filters start again from page 1. */
    setFilters: (name: string, tag: string) => {
      go({ ...params, page: 1, name: name.trim(), tag: tag.trim() });
    },
  };
}

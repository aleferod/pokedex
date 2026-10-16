export const PAGE_SIZES = [20, 40, 60] as const;
export const DEFAULT_SIZE = 20;

/** `?page` is 1-based; anything that is not a whole number from 1 up means page 1. */
export function parsePage(value: string | null): number {
  if (value === null || !/^\d{1,6}$/.test(value)) {
    return 1;
  }
  const page = Number(value);
  return page >= 1 ? page : 1;
}

/** `?size` is one of the offered sizes, otherwise the default. */
export function parseSize(value: string | null): number {
  const size = Number(value);
  return (PAGE_SIZES as readonly number[]).includes(size) ? size : DEFAULT_SIZE;
}

const NAME = /^[a-z0-9-]{3,50}$/;

/**
 * The search from `?name`, normalised like pokedex-api does (trim, lower case, a hyphen for each
 * run of spaces). `''` when absent or when the API would reject it, like nonsense in `?page`.
 */
export function parseName(value: string | null): string {
  if (value === null) {
    return '';
  }
  const normalised = value.trim().toLowerCase().replace(/\s+/g, '-');
  return NAME.test(normalised) ? normalised : '';
}

/**
 * The query string for a page, size and search, leaving defaults out (`/pokemon` is page 1 of 20
 * without a search).
 */
export function catalogSearch(page: number, size: number, name = ''): string {
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
  const search = params.toString();
  return search === '' ? '' : `?${search}`;
}

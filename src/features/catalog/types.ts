/** One catalog entry as pokedex-api returns it (US01). */
export interface CatalogEntry {
  id: number;
  name: string;
  spriteUrl: string | null;
  category: string | null;
  weight: number;
  abilities: string[];
}

/** The API's paginated envelope (`page` is 0-based there). */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

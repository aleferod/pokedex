/** One catalog entry as pokedex-api returns it (US01). */
export interface CatalogEntry {
  id: number;
  name: string;
  spriteUrl: string | null;
  category: string | null;
  weight: number;
  abilities: string[];
}

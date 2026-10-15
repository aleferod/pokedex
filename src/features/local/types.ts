import type { BaseStats } from '@/features/detail';

/** A record of the local Pokédex, as pokedex-api returns it (US03/US04). */
export interface LocalPokemon {
  id: string;
  pokeapiId: number;
  // Upstream (PokeAPI), read-only.
  name: string;
  spriteUrl: string | null;
  imageUrl: string | null;
  category: string | null;
  types: string[];
  height: number;
  weight: number;
  abilities: string[];
  stats: BaseStats;
  // Proprietary.
  localizedNames: Record<string, string>;
  region: string | null;
  habitatNotes: string | null;
  internalTags: string[];
  notes: string | null;
  // Audit.
  createdAt: string;
  updatedAt: string;
  syncedAt: string;
  createdBy: string;
  /** The optimistic-locking version; the API's ETag is this number in quotes. */
  version: number;
}

/** What `POST /local-pokemon/sync?limit=N` reports. */
export interface BulkSyncResult {
  results: { pokeapiId: number; name: string; status: 'created' | 'updated' }[];
  created: number;
  updated: number;
}

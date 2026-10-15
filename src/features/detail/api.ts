import { apiRequest } from '@/shared/api';

import type { PokemonDetail } from './types';

/** One Pokémon by canonical identifier: a Pokédex number or a lower-case name. */
export function fetchPokemonDetail(
  identifier: string,
  signal?: AbortSignal,
): Promise<PokemonDetail> {
  return apiRequest<PokemonDetail>(
    `/pokemon/${encodeURIComponent(identifier)}`,
    signal ? { signal } : {},
  );
}

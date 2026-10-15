import { useQuery } from '@tanstack/react-query';

import { fetchPokemonDetail } from './api';

/**
 * The detail of one Pokémon. No placeholder data: showing the previous Pokémon under the new
 * address would mislabel it, so the skeleton shows while another one loads.
 */
export function usePokemonDetail(identifier: string) {
  return useQuery({
    queryKey: ['pokemon', identifier],
    queryFn: ({ signal }) => fetchPokemonDetail(identifier, signal),
  });
}

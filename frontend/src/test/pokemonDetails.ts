/** Pokémon details shaped like pokedex-api's `GET /pokemon/{idOrName}` answer, for MSW handlers. */

export interface StageFixture {
  id: number;
  name: string;
  condition: string | null;
  evolvesTo: StageFixture[];
}

export const ARTWORK =
  'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other';

export function stage(
  id: number,
  name: string,
  condition: string | null,
  evolvesTo: StageFixture[] = [],
): StageFixture {
  return { id, name, condition, evolvesTo };
}

/** Bulbasaur's numbers and texts under another id, name and chain, unless overridden. */
export function pokemonDetail(
  id: number,
  name: string,
  evolutionChain: StageFixture = stage(id, name, null),
  overrides: object = {},
) {
  return {
    id,
    name,
    imageUrl: `${ARTWORK}/official-artwork/${String(id)}.png`,
    category: 'Seed Pokémon',
    description: 'A strange seed was planted on its back at birth.',
    types: ['grass', 'poison'],
    height: 0.7,
    weight: 6.9,
    abilities: ['overgrow', 'chlorophyll'],
    stats: { hp: 45, attack: 49, defense: 49, specialAttack: 65, specialDefense: 65, speed: 45 },
    evolutionChain,
    ...overrides,
  };
}

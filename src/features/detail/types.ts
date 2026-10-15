/** The six base stats, as pokedex-api names them. */
export interface BaseStats {
  hp: number;
  attack: number;
  defense: number;
  specialAttack: number;
  specialDefense: number;
  speed: number;
}

/** One stage of an evolution family; `condition` is null for the base form. */
export interface EvolutionStage {
  id: number;
  name: string;
  condition: string | null;
  evolvesTo: EvolutionStage[];
}

/** One Pokémon in full, as pokedex-api returns it (US02). Height in metres, weight in kilograms. */
export interface PokemonDetail {
  id: number;
  name: string;
  imageUrl: string | null;
  category: string | null;
  description: string | null;
  types: string[];
  height: number;
  weight: number;
  abilities: string[];
  stats: BaseStats;
  evolutionChain: EvolutionStage;
}

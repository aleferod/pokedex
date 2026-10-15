/*
 * pokedex-api's PokemonIdentifier rule: a Pokédex number from 1 to 99999 (leading zeros allowed)
 * or a lower-case name of letters, digits and hyphens starting with a letter, up to 50 characters.
 */

export const IDENTIFIER_RULE = 'Enter a Pokédex number or a name such as pikachu';

const NUMBER = /^\d{1,9}$/;
const NAME = /^[a-z][a-z0-9-]{0,49}$/;
const MAX_NUMBER = 99_999;

/** The identifier as the API path expects it (`025` → `25`, `Pikachu` → `pikachu`), or null. */
export function parsePokemonIdentifier(raw: string): string | null {
  const value = raw.trim().toLowerCase();
  if (NUMBER.test(value)) {
    const number = Number(value);
    return number >= 1 && number <= MAX_NUMBER ? String(number) : null;
  }
  return NAME.test(value) ? value : null;
}

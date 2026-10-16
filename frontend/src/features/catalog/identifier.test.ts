import { describe, expect, it } from 'vitest';

import { parsePokemonIdentifier } from './identifier';

describe('Pokémon identifier (mirrors pokedex-api PokemonIdentifier)', () => {
  it.each([
    ['25', '25'],
    ['025', '25'],
    ['0000025', '25'],
    ['1', '1'],
    ['99999', '99999'],
    [' 133 ', '133'],
  ])('reads the number %j as %j', (raw, expected) => {
    expect(parsePokemonIdentifier(raw)).toBe(expected);
  });

  it.each([
    ['Pikachu', 'pikachu'],
    ['  MR-MIME ', 'mr-mime'],
    ['porygon2', 'porygon2'],
    ['a'.repeat(50), 'a'.repeat(50)],
  ])('reads the name %j as %j', (raw, expected) => {
    expect(parsePokemonIdentifier(raw)).toBe(expected);
  });

  it.each([
    '',
    '   ',
    '0',
    '100000',
    '-1',
    'pika chu',
    '2abc',
    '-pikachu',
    'a'.repeat(51),
    'pika!',
  ])('rejects %j', (raw) => {
    expect(parsePokemonIdentifier(raw)).toBeNull();
  });
});

package com.ballastlane.pokedex.application.port.in.localpokemon;

/** Input of {@link CreateLocalPokemon}: which Pokémon to store and its first proprietary values. */
public record CreateLocalPokemonCommand(int pokeapiId, ProprietaryValues proprietary) {}

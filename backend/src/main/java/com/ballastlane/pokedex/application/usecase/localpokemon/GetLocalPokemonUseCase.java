package com.ballastlane.pokedex.application.usecase.localpokemon;

import com.ballastlane.pokedex.application.port.in.localpokemon.GetLocalPokemon;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException;

/** Reads one local record. */
public class GetLocalPokemonUseCase implements GetLocalPokemon {

  private final LocalPokemonRepositoryPort repository;

  public GetLocalPokemonUseCase(LocalPokemonRepositoryPort repository) {
    this.repository = repository;
  }

  @Override
  public LocalPokemon get(LocalPokemonId id) {
    return repository.findById(id).orElseThrow(() -> new LocalPokemonNotFoundException(id));
  }
}

package com.ballastlane.pokedex.application.usecase.localpokemon;

import com.ballastlane.pokedex.application.port.in.localpokemon.DeleteLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.ExpectedVersion;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException;

/** Removes a local record, honouring the client's expected version when it gives one. */
public class DeleteLocalPokemonUseCase implements DeleteLocalPokemon {

  private final LocalPokemonRepositoryPort repository;

  public DeleteLocalPokemonUseCase(LocalPokemonRepositoryPort repository) {
    this.repository = repository;
  }

  @Override
  public void delete(LocalPokemonId id, ExpectedVersion expected) {
    LocalPokemon current =
        repository.findById(id).orElseThrow(() -> new LocalPokemonNotFoundException(id));
    expected.verify(current);
    repository.delete(current);
  }
}

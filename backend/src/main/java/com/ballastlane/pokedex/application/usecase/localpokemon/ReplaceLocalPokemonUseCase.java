package com.ballastlane.pokedex.application.usecase.localpokemon;

import com.ballastlane.pokedex.application.port.in.localpokemon.ExpectedVersion;
import com.ballastlane.pokedex.application.port.in.localpokemon.ProprietaryValues;
import com.ballastlane.pokedex.application.port.in.localpokemon.ReplaceLocalPokemon;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException;
import java.time.Clock;

/**
 * Replaces every proprietary field (PUT). The version is checked here against the client's
 * expectation and again by the store at save, which catches a writer that slipped in between.
 */
public class ReplaceLocalPokemonUseCase implements ReplaceLocalPokemon {

  private final LocalPokemonRepositoryPort repository;
  private final Clock clock;

  public ReplaceLocalPokemonUseCase(LocalPokemonRepositoryPort repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  public LocalPokemon replace(
      LocalPokemonId id, ExpectedVersion expected, ProprietaryValues values) {
    LocalPokemon current =
        repository.findById(id).orElseThrow(() -> new LocalPokemonNotFoundException(id));
    expected.verify(current);
    return repository.save(current.withProprietary(values.toProprietaryData(), clock.instant()));
  }
}

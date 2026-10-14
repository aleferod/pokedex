package com.ballastlane.pokedex.application.usecase.localpokemon;

import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemonCommand;
import com.ballastlane.pokedex.application.port.in.pokemon.GetPokemonDetail;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import com.ballastlane.pokedex.domain.localpokemon.UpstreamData;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonNotFoundException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldsException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores a new local record. Checks run cheapest first, so bad input never costs a PokeAPI call:
 * the client's values (every violation together), then the duplicate check, then PokeAPI. The
 * store's unique key still settles a race between two creators (409).
 */
public class CreateLocalPokemonUseCase implements CreateLocalPokemon {

  static final String POKEAPI_ID = "pokeapiId";

  private final GetPokemonDetail getPokemonDetail;
  private final LocalPokemonRepositoryPort repository;
  private final Clock clock;

  public CreateLocalPokemonUseCase(
      GetPokemonDetail getPokemonDetail, LocalPokemonRepositoryPort repository, Clock clock) {
    this.getPokemonDetail = getPokemonDetail;
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  public LocalPokemon create(CreateLocalPokemonCommand command, String actor) {
    List<InvalidFieldException> violations = new ArrayList<>();
    PokemonId pokeapiId = null;
    try {
      pokeapiId = new PokemonId(command.pokeapiId());
    } catch (InvalidFieldException e) {
      violations.add(new InvalidFieldException(POKEAPI_ID, e.reason()));
    }
    ProprietaryData proprietary = null;
    try {
      proprietary = command.proprietary().toProprietaryData();
    } catch (InvalidFieldsException e) {
      violations.addAll(e.violations());
    }
    if (!violations.isEmpty()) {
      throw new InvalidFieldsException(violations);
    }

    if (repository.existsByPokeapiId(pokeapiId)) {
      throw new DuplicateLocalPokemonException(pokeapiId);
    }
    PokemonDetail detail = fetch(pokeapiId);
    Instant now = clock.instant();
    return repository.save(
        LocalPokemon.importFrom(
                LocalPokemonId.random(), detail.id(), UpstreamData.from(detail), actor, now)
            .withProprietary(proprietary, now));
  }

  private PokemonDetail fetch(PokemonId pokeapiId) {
    try {
      return getPokemonDetail.get(new PokemonIdentifier.ById(pokeapiId));
    } catch (PokemonNotFoundException unknown) {
      throw new InvalidFieldException(POKEAPI_ID, "no Pokémon has this number");
    }
  }
}

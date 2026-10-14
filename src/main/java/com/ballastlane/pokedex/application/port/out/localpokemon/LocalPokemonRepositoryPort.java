package com.ballastlane.pokedex.application.port.out.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import java.util.Optional;

/** Repository port for the local replica. */
public interface LocalPokemonRepositoryPort {

  Optional<LocalPokemon> findById(LocalPokemonId id);

  Optional<LocalPokemon> findByPokeapiId(PokemonId pokeapiId);

  /**
   * Inserts a record whose version is {@code null}, otherwise updates it if its version is still
   * current. Returns the stored record with its new version.
   *
   * @throws com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException another
   *     record already has this PokeAPI number
   * @throws com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException the record
   *     changed since it was read
   */
  LocalPokemon save(LocalPokemon pokemon);
}

package com.ballastlane.pokedex.application.port.out.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFilter;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.Optional;

/** Repository port for the local replica. */
public interface LocalPokemonRepositoryPort {

  Optional<LocalPokemon> findById(LocalPokemonId id);

  Optional<LocalPokemon> findByPokeapiId(PokemonId pokeapiId);

  boolean existsByPokeapiId(PokemonId pokeapiId);

  /** Records matching every criterion of {@code filter}, ordered by PokeAPI number. */
  Page<LocalPokemon> findAll(LocalPokemonFilter filter, PageRequest page);

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

  /**
   * Removes the record if its version is still current.
   *
   * @throws com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException the record
   *     changed (or was removed) since it was read
   */
  void delete(LocalPokemon pokemon);
}

package com.ballastlane.pokedex.adapter.out.persistence.localpokemon;

import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFilter;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository adapter: implements {@link LocalPokemonRepositoryPort} on PostgreSQL. Optimistic
 * locking ({@code @Version}) becomes {@link StaleLocalPokemonException}; the unique PokeAPI number
 * becomes {@link DuplicateLocalPokemonException}. Flushes inside {@link #save} so both surface
 * here.
 */
@Component
public class LocalPokemonPersistenceAdapter implements LocalPokemonRepositoryPort {

  private final SpringDataLocalPokemonRepository repository;

  LocalPokemonPersistenceAdapter(SpringDataLocalPokemonRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<LocalPokemon> findById(LocalPokemonId id) {
    return repository.findById(id.value()).map(LocalPokemonPersistenceMapper::toDomain);
  }

  @Override
  public Optional<LocalPokemon> findByPokeapiId(PokemonId pokeapiId) {
    return repository
        .findByPokeapiId(pokeapiId.value())
        .map(LocalPokemonPersistenceMapper::toDomain);
  }

  @Override
  public LocalPokemon save(LocalPokemon pokemon) {
    try {
      return LocalPokemonPersistenceMapper.toDomain(
          repository.saveAndFlush(LocalPokemonPersistenceMapper.toEntity(pokemon)));
    } catch (OptimisticLockingFailureException stale) {
      throw new StaleLocalPokemonException(pokemon.id());
    } catch (DataIntegrityViolationException violation) {
      String detail =
          String.valueOf(violation.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
      if (detail.contains("uk_local_pokemon_pokeapi_id")) {
        throw new DuplicateLocalPokemonException(pokemon.pokeapiId());
      }
      throw violation;
    }
  }

  @Override
  public boolean existsByPokeapiId(PokemonId pokeapiId) {
    return repository.existsByPokeapiId(pokeapiId.value());
  }

  @Override
  public Page<LocalPokemon> findAll(LocalPokemonFilter filter, PageRequest page) {
    var rows =
        repository.findFiltered(
            filter.tag(),
            containsPattern(filter.name()),
            org.springframework.data.domain.PageRequest.of(page.page(), page.size()));
    return Page.of(
        rows.getContent().stream().map(LocalPokemonPersistenceMapper::toDomain).toList(),
        page,
        rows.getTotalElements());
  }

  @Override
  @Transactional
  public void delete(LocalPokemon pokemon) {
    try {
      // Merging the detached copy fails if its version is not the stored one; the DELETE itself
      // is also guarded by "WHERE version = ?", which catches a writer in between.
      repository.delete(LocalPokemonPersistenceMapper.toEntity(pokemon));
      repository.flush();
    } catch (OptimisticLockingFailureException stale) {
      throw new StaleLocalPokemonException(pokemon.id());
    }
  }

  /** {@code %value%} for ILIKE, with the wildcards and the escape character taken literally. */
  private static String containsPattern(String value) {
    if (value == null) {
      return null;
    }
    String escaped = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + escaped + "%";
  }
}

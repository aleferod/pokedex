package com.ballastlane.pokedex.application.port.out.pokemon;

import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.Optional;

/**
 * The upstream Pokémon catalog (PokeAPI today). Returns finished summaries: how many upstream calls
 * that takes is the adapter's business, not the application's.
 */
public interface PokemonCatalogPort {

  /**
   * @throws CatalogUnavailableException the source is unreachable, too slow or failing
   */
  Page<PokemonSummary> findPage(PageRequest request);

  /**
   * The full detail of one Pokémon; empty when no Pokémon matches.
   *
   * @throws CatalogUnavailableException the source cannot answer right now
   */
  Optional<PokemonDetail> findDetail(PokemonIdentifier identifier);

  /**
   * Like {@link #findDetail}, but straight from the source: an implementation that caches must
   * bypass its cache, and should keep what it read. Used when the answer is persisted (the local
   * replica), where a stale copy would be stored for good. Without a cache it is {@link
   * #findDetail}.
   *
   * @throws CatalogUnavailableException the source cannot answer right now
   */
  default Optional<PokemonDetail> findFreshDetail(PokemonIdentifier identifier) {
    return findDetail(identifier);
  }
}

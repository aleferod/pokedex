package com.ballastlane.pokedex.adapter.out.cache;

import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.domain.pokemon.CatalogName;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.cache.Cache;

/**
 * Decorator: a {@link PokemonCatalogPort} that remembers what another one answered, in two Spring
 * caches (Caffeine in production). Pages are keyed by {@link PageRequest}; details by number and by
 * name, so either finds an entry loaded through the other. An unknown Pokémon is remembered as
 * {@link Optional#empty()}. A failure is rethrown unchanged and nothing is stored. {@link
 * #findFreshDetail} skips the cache and then stores what it read (write-through).
 *
 * <p>Each load runs on the calling thread inside the cache's per-key compute: callers for the same
 * key wait for one load, and the request's MDC stays in place for the upstream calls.
 */
public class CachingPokemonCatalog implements PokemonCatalogPort {

  public static final String PAGES = "pokemon-pages";
  public static final String DETAILS = "pokemon-details";
  public static final String NAMES = "pokemon-names";

  private static final String ALL_NAMES = "all";

  private final PokemonCatalogPort delegate;
  private final Cache pages;
  private final Cache details;
  private final Cache names;

  public CachingPokemonCatalog(
      PokemonCatalogPort delegate, Cache pages, Cache details, Cache names) {
    this.delegate = delegate;
    this.pages = pages;
    this.details = details;
    this.names = names;
  }

  @Override
  public Page<PokemonSummary> findPage(PageRequest request) {
    return load(pages, request, () -> delegate.findPage(request));
  }

  @Override
  public Optional<PokemonDetail> findDetail(PokemonIdentifier identifier) {
    AtomicBoolean loaded = new AtomicBoolean();
    Optional<PokemonDetail> detail =
        load(
            details,
            identifier.value(),
            () -> {
              loaded.set(true);
              return delegate.findDetail(identifier);
            });
    if (loaded.get()) {
      // Only after a real load: re-putting on every hit would reset the entry's write time.
      detail.ifPresent(found -> storeUnderOtherKeys(found, identifier.value()));
    }
    return detail;
  }

  @Override
  public Optional<PokemonDetail> findFreshDetail(PokemonIdentifier identifier) {
    Optional<PokemonDetail> fresh = delegate.findFreshDetail(identifier);
    fresh.ifPresent(found -> storeUnderOtherKeys(found, null));
    return fresh;
  }

  /** Stores {@code detail} under its number and name, except {@code loadedKey}. */
  private void storeUnderOtherKeys(PokemonDetail detail, String loadedKey) {
    Optional<PokemonDetail> value = Optional.of(detail);
    for (String key : List.of(String.valueOf(detail.id().value()), detail.name())) {
      if (!key.equals(loadedKey)) {
        details.put(key, value);
      }
    }
  }

  private static <T> T load(Cache cache, Object key, Callable<T> loader) {
    try {
      return cache.get(key, loader);
    } catch (Cache.ValueRetrievalException wrapped) {
      // Callers keep seeing the port's own exceptions (CatalogUnavailableException → 503).
      if (wrapped.getCause() instanceof RuntimeException failure) {
        throw failure;
      }
      throw wrapped;
    }
  }

  /** The whole index under one key: a search filters it in memory. */
  @Override
  public List<CatalogName> findAllNames() {
    return load(names, ALL_NAMES, delegate::findAllNames);
  }

  /** Keyed by the numbers asked for: the same search page asks for the same numbers. */
  @Override
  public List<PokemonSummary> findSummaries(List<PokemonId> ids) {
    return load(pages, List.copyOf(ids), () -> delegate.findSummaries(ids));
  }
}

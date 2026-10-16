package com.ballastlane.pokedex.domain.localpokemon;

import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate: a locally owned copy of one Pokémon. Upstream data comes from PokeAPI and is replaced
 * by synchronization only; proprietary data belongs to the business and synchronization never
 * touches it. {@code version} is the optimistic-locking token ({@code null} until first stored).
 */
public record LocalPokemon(
    LocalPokemonId id,
    PokemonId pokeapiId,
    UpstreamData upstream,
    ProprietaryData proprietary,
    AuditInfo audit,
    Long version) {

  public LocalPokemon {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(pokeapiId, "pokeapiId");
    Objects.requireNonNull(upstream, "upstream");
    Objects.requireNonNull(audit, "audit");
    proprietary = proprietary == null ? ProprietaryData.empty() : proprietary;
  }

  /** Factory: first import from PokeAPI, with no proprietary data yet. */
  public static LocalPokemon importFrom(
      LocalPokemonId id, PokemonId pokeapiId, UpstreamData upstream, String actor, Instant now) {
    return new LocalPokemon(
        id,
        pokeapiId,
        upstream,
        ProprietaryData.empty(),
        new AuditInfo(now, now, now, actor),
        null);
  }

  /** Business edit: new proprietary data; upstream data and the last sync time are unchanged. */
  public LocalPokemon withProprietary(ProprietaryData newProprietary, Instant now) {
    return new LocalPokemon(
        id,
        pokeapiId,
        upstream,
        newProprietary,
        new AuditInfo(audit.createdAt(), now, audit.syncedAt(), audit.createdBy()),
        version);
  }

  /**
   * Optimistic Offline Lock: the caller read version {@code expected}.
   *
   * @throws StaleLocalPokemonException when the record has moved on (or was never stored)
   */
  public void requireVersion(long expected) {
    if (version == null || version != expected) {
      throw new StaleLocalPokemonException(id);
    }
  }

  /** Re-sync: new upstream data; proprietary data and creation audit are kept as they are. */
  public LocalPokemon resyncWith(UpstreamData newUpstream, Instant now) {
    return new LocalPokemon(
        id,
        pokeapiId,
        newUpstream,
        proprietary,
        new AuditInfo(audit.createdAt(), now, now, audit.createdBy()),
        version);
  }
}

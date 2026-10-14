package com.ballastlane.pokedex.adapter.out.persistence.localpokemon;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access to {@code local_pokemon}. */
interface SpringDataLocalPokemonRepository extends JpaRepository<LocalPokemonJpaEntity, UUID> {

  /**
   * Native SQL so the tag filter is array containment ({@code @>}), which the GIN index on {@code
   * internal_tags} serves. Every parameter is cast, so PostgreSQL knows its type even when null.
   * {@code namePattern} is an already escaped {@code ILIKE} pattern ({@code \} escapes).
   */
  String FILTER =
      """
      FROM local_pokemon
      WHERE (CAST(:tag AS text) IS NULL OR internal_tags @> ARRAY[CAST(:tag AS text)])
        AND (CAST(:namePattern AS text) IS NULL
             OR name ILIKE CAST(:namePattern AS text) ESCAPE '\\')
      """;

  Optional<LocalPokemonJpaEntity> findByPokeapiId(int pokeapiId);

  boolean existsByPokeapiId(int pokeapiId);

  @Query(
      value = "SELECT * " + FILTER + " ORDER BY pokeapi_id",
      countQuery = "SELECT count(*) " + FILTER,
      nativeQuery = true)
  Page<LocalPokemonJpaEntity> findFiltered(
      @Param("tag") String tag, @Param("namePattern") String namePattern, Pageable pageable);
}

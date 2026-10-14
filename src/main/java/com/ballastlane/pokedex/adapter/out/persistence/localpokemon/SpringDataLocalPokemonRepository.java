package com.ballastlane.pokedex.adapter.out.persistence.localpokemon;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to {@code local_pokemon}. */
interface SpringDataLocalPokemonRepository extends JpaRepository<LocalPokemonJpaEntity, UUID> {

  Optional<LocalPokemonJpaEntity> findByPokeapiId(int pokeapiId);
}

package com.ballastlane.pokedex.adapter.out.persistence.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to {@code users}; values arrive already lower-cased by the domain. */
interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  Optional<UserJpaEntity> findByUsername(String username);
}

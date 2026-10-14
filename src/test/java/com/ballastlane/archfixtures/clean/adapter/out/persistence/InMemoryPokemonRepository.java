package com.ballastlane.archfixtures.clean.adapter.out.persistence;

import com.ballastlane.archfixtures.clean.application.port.out.PokemonRepository;
import com.ballastlane.archfixtures.clean.domain.Pokemon;
import java.util.List;
import java.util.Optional;

public class InMemoryPokemonRepository implements PokemonRepository {

  @Override
  public Optional<Pokemon> findById(int id) {
    return Optional.of(new Pokemon(id, "pikachu", List.of("static")));
  }
}

package com.ballastlane.archfixtures.clean.application.usecase;

import com.ballastlane.archfixtures.clean.application.port.in.GetPokemon;
import com.ballastlane.archfixtures.clean.application.port.out.PokemonRepository;
import com.ballastlane.archfixtures.clean.domain.Pokemon;
import java.util.Optional;

public class GetPokemonUseCase implements GetPokemon {

  private final PokemonRepository repository;

  public GetPokemonUseCase(PokemonRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<Pokemon> byId(int id) {
    return repository.findById(id);
  }
}

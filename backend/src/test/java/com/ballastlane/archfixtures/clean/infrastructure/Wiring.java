package com.ballastlane.archfixtures.clean.infrastructure;

import com.ballastlane.archfixtures.clean.adapter.in.web.PokemonController;
import com.ballastlane.archfixtures.clean.adapter.out.persistence.InMemoryPokemonRepository;
import com.ballastlane.archfixtures.clean.application.usecase.GetPokemonUseCase;

public final class Wiring {

  private Wiring() {}

  public static PokemonController controller() {
    return new PokemonController(new GetPokemonUseCase(new InMemoryPokemonRepository()));
  }
}

package com.ballastlane.archfixtures.clean.application.port.out;

import com.ballastlane.archfixtures.clean.domain.Pokemon;
import java.util.Optional;

public interface PokemonRepository {
  Optional<Pokemon> findById(int id);
}

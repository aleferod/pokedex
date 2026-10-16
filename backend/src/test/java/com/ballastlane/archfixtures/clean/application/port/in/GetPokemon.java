package com.ballastlane.archfixtures.clean.application.port.in;

import com.ballastlane.archfixtures.clean.domain.Pokemon;
import java.util.Optional;

public interface GetPokemon {
  Optional<Pokemon> byId(int id);
}

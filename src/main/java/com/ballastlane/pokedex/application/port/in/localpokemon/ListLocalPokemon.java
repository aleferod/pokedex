package com.ballastlane.pokedex.application.port.in.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFilter;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;

/** Use case (US04): page through the local records, optionally filtered, by PokeAPI number. */
public interface ListLocalPokemon {

  Page<LocalPokemon> list(LocalPokemonFilter filter, PageRequest page);
}

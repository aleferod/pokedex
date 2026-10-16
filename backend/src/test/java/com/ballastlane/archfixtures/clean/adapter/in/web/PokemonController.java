package com.ballastlane.archfixtures.clean.adapter.in.web;

import com.ballastlane.archfixtures.clean.application.port.in.GetPokemon;

public class PokemonController {

  private final GetPokemon getPokemon;

  public PokemonController(GetPokemon getPokemon) {
    this.getPokemon = getPokemon;
  }

  public String name(int id) {
    return getPokemon.byId(id).map(p -> p.name()).orElse("unknown");
  }
}

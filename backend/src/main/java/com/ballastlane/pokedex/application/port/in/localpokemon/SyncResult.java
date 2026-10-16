package com.ballastlane.pokedex.application.port.in.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;

/** Outcome of a sync: the stored record and whether it was created (first import) or updated. */
public record SyncResult(LocalPokemon pokemon, boolean created) {}

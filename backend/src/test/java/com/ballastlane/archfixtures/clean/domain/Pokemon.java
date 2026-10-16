package com.ballastlane.archfixtures.clean.domain;

import java.util.List;

public record Pokemon(int id, String name, List<String> abilities) {

  public Pokemon {
    if (id <= 0) {
      throw new IllegalArgumentException("id must be positive");
    }
    abilities = List.copyOf(abilities);
  }
}

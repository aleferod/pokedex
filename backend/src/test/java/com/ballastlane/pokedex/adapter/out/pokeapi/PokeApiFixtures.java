package com.ballastlane.pokedex.adapter.out.pokeapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;

/** Loads the trimmed, real PokeAPI captures from {@code src/test/resources/pokeapi}. */
public final class PokeApiFixtures {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private PokeApiFixtures() {}

  public static String json(String name) {
    try (InputStream in = PokeApiFixtures.class.getResourceAsStream("/pokeapi/" + name)) {
      if (in == null) {
        throw new IllegalArgumentException("No fixture " + name);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException unreadable) {
      throw new UncheckedIOException(unreadable);
    }
  }

  public static <T> T read(String name, Class<T> type) {
    return JSON.readValue(json(name), type);
  }
}

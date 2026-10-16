package com.ballastlane.pokedex.adapter.in.seed;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the demo data file. Unknown members are errors, so a typo in the file stops startup instead
 * of silently seeding less than intended.
 */
public final class DemoDataReader {

  private static final JsonMapper JSON =
      JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private DemoDataReader() {}

  /**
   * @throws IllegalStateException the file is missing, unreadable or malformed (names the file)
   */
  public static DemoData read(Resource resource) {
    try (InputStream in = resource.getInputStream()) {
      return JSON.readValue(in, DemoData.class);
    } catch (IOException | JacksonException failure) {
      throw new IllegalStateException(
          "Cannot read demo data from " + resource.getDescription() + ": " + failure.getMessage(),
          failure);
    }
  }
}

package com.ballastlane.pokedex.infrastructure.config;

import java.util.List;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Stops startup before any bean is created when a required setting has no value, naming the missing
 * variable. Without it, Spring's binder keeps an unresolved {@code ${DB_PASSWORD}} as the literal
 * password and the problem only surfaces later as a misleading database authentication error. The
 * {@code test} profile is exempt: Testcontainers supplies the connection there.
 */
public class RequiredSettingsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  static final List<String> REQUIRED_OUTSIDE_TESTS =
      List.of("spring.datasource.password", "app.security.jwt.secret");

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (environment.matchesProfiles("test")) {
      return;
    }
    for (String key : REQUIRED_OUTSIDE_TESTS) {
      String value;
      try {
        value = environment.getRequiredProperty(key);
      } catch (IllegalArgumentException | IllegalStateException missing) {
        throw new IllegalStateException(
            "Missing required configuration: "
                + missing.getMessage()
                + ". Set it in the environment or in .env (see .env.example).",
            missing);
      }
      if (value.isBlank()) {
        throw new IllegalStateException(
            "Missing required configuration: '" + key + "' is blank (see .env.example).");
      }
    }
  }
}

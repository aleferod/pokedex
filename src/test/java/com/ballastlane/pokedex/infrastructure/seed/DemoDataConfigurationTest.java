package com.ballastlane.pokedex.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ballastlane.pokedex.adapter.in.seed.DemoDataSeeder;
import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemon;
import com.ballastlane.pokedex.application.port.in.user.ProvisionUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DemoDataConfigurationTest {

  private final ApplicationContextRunner context =
      new ApplicationContextRunner()
          .withUserConfiguration(DemoDataConfiguration.class)
          .withBean(ProvisionUser.class, () -> mock(ProvisionUser.class))
          .withBean(CreateLocalPokemon.class, () -> mock(CreateLocalPokemon.class));

  @Test
  void should_notSeed_when_demoDataIsNotEnabled() {
    context.run(started -> assertThat(started).doesNotHaveBean(DemoDataSeeder.class));
  }

  @Test
  void should_notSeed_when_demoDataIsExplicitlyDisabled() {
    context
        .withPropertyValues("app.seed.enabled=false")
        .run(started -> assertThat(started).doesNotHaveBean(DemoDataSeeder.class));
  }

  @Test
  void should_seedTheShippedFile_when_enabled() {
    context
        .withPropertyValues("app.seed.enabled=true")
        .run(started -> assertThat(started).hasSingleBean(DemoDataSeeder.class));
  }

  @Test
  void should_failToStart_when_theSeedFileIsMissing() {
    context
        .withPropertyValues("app.seed.enabled=true", "app.seed.location=classpath:seed/nope.json")
        .run(
            started ->
                assertThat(started)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("seed/nope.json"));
  }
}

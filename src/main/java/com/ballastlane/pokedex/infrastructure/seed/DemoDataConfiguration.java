package com.ballastlane.pokedex.infrastructure.seed;

import com.ballastlane.pokedex.adapter.in.seed.DemoDataReader;
import com.ballastlane.pokedex.adapter.in.seed.DemoDataSeeder;
import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemon;
import com.ballastlane.pokedex.application.port.in.user.ProvisionUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * Demo data: the seeder exists only when {@code app.seed.enabled} is true. The file is read while
 * the context starts, so a missing or malformed file stops startup with its name.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SeedProperties.class)
public class DemoDataConfiguration {

  @Bean
  @ConditionalOnBooleanProperty("app.seed.enabled")
  DemoDataSeeder demoDataSeeder(
      SeedProperties properties,
      ResourceLoader resources,
      ProvisionUser provisionUser,
      CreateLocalPokemon createLocalPokemon) {
    return new DemoDataSeeder(
        DemoDataReader.read(resources.getResource(properties.location())),
        provisionUser,
        createLocalPokemon);
  }
}

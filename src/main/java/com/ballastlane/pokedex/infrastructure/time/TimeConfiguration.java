package com.ballastlane.pokedex.infrastructure.time;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single source of "now", so time-dependent code can be tested with a fixed clock. Ticks in whole
 * microseconds, the precision PostgreSQL stores, so an entity read back equals the one saved.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

  @Bean
  Clock clock() {
    return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
  }
}

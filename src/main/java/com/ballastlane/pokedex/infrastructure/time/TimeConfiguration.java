package com.ballastlane.pokedex.infrastructure.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Single source of "now", so time-dependent code can be tested with a fixed clock. */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}

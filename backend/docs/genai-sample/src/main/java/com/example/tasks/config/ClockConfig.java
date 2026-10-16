package com.example.tasks.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

  /**
   * UTC, ticking in microseconds: PostgreSQL stores timestamps to the microsecond, so a value
   * returned on create reads back identically later.
   */
  @Bean
  public Clock clock() {
    return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
  }
}

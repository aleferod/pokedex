package com.ballastlane.pokedex.infrastructure.web;

import com.ballastlane.pokedex.adapter.in.web.correlation.CorrelationIdFilter;
import com.ballastlane.pokedex.adapter.in.web.cors.ProblemDetailCorsFilter;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailFactory;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailWriter;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cross-origin policy for {@code /api/**}: only the configured origins, the methods and headers the
 * frontend needs, and the response headers it must be able to read. The single {@link
 * CorsConfigurationSource} bean is the one place these rules are defined.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
public class CrossOriginConfiguration {

  private static final List<String> ALLOWED_METHODS =
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
  private static final List<String> ALLOWED_HEADERS =
      List.of(
          HttpHeaders.AUTHORIZATION,
          HttpHeaders.CONTENT_TYPE,
          HttpHeaders.IF_MATCH,
          CorrelationIdFilter.HEADER);
  private static final List<String> EXPOSED_HEADERS =
      List.of(HttpHeaders.LOCATION, HttpHeaders.ETAG, CorrelationIdFilter.HEADER);

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration api = new CorsConfiguration();
    api.setAllowedOrigins(properties.allowedOrigins());
    api.setAllowedMethods(ALLOWED_METHODS);
    api.setAllowedHeaders(ALLOWED_HEADERS);
    api.setExposedHeaders(EXPOSED_HEADERS);
    // Tokens travel in the Authorization header, never in cookies.
    api.setAllowCredentials(false);
    api.setMaxAge(Duration.ofHours(1));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", api);
    return source;
  }

  /** Right after the correlation id filter, so rejected requests still carry the id. */
  @Bean
  FilterRegistrationBean<ProblemDetailCorsFilter> corsFilter(
      CorsConfigurationSource corsConfigurationSource,
      ProblemDetailFactory problems,
      ProblemDetailWriter writer) {
    FilterRegistrationBean<ProblemDetailCorsFilter> registration =
        new FilterRegistrationBean<>(
            new ProblemDetailCorsFilter(corsConfigurationSource, problems, writer));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return registration;
  }
}

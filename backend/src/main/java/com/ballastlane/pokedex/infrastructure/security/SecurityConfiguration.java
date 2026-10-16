package com.ballastlane.pokedex.infrastructure.security;

import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailFactory;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailWriter;
import com.ballastlane.pokedex.adapter.in.web.security.ProblemDetailAccessDeniedHandler;
import com.ballastlane.pokedex.adapter.in.web.security.ProblemDetailAuthenticationEntryPoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who may call what. Stateless bearer-token API: no session, no cookies, so no CSRF; JWTs are
 * verified by the OAuth2 resource server. The protected areas are listed explicitly (see design.md
 * D8): everything else stays public so the catalog needs no token and unknown routes still answer
 * 404. {@code AccessControlTest} pins every rule; new protected routes go here.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {

  /** The local replica (US03/US04): any authenticated caller; deleting needs ADMIN. */
  static final String LOCAL_POKEMON = "/api/v1/local-pokemon/**";

  static final String CURRENT_USER = "/api/v1/auth/me";

  @Bean
  SecurityFilterChain apiSecurity(
      HttpSecurity http,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      ProblemDetailFactory problems,
      ProblemDetailWriter writer)
      throws Exception {
    ProblemDetailAuthenticationEntryPoint unauthorized =
        new ProblemDetailAuthenticationEntryPoint(problems, writer);
    ProblemDetailAccessDeniedHandler forbidden =
        new ProblemDetailAccessDeniedHandler(problems, writer);
    return http
        // CORS is handled by ProblemDetailCorsFilter, which runs before this chain.
        .cors(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(HttpMethod.DELETE, LOCAL_POKEMON)
                    .hasRole("ADMIN")
                    .requestMatchers(LOCAL_POKEMON, CURRENT_USER)
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(unauthorized)
                    .accessDeniedHandler(forbidden))
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
        .build();
  }
}

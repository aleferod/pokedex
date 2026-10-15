package com.example.tasks.security;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper json) throws Exception {
    // Bearer entry point: 401 plus the RFC 6750 challenge (error="invalid_token" for bad tokens)
    AuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
    AuthenticationEntryPoint problem =
        (request, response, exception) -> {
          bearer.commence(request, response, exception);
          Map<String, Object> body = new LinkedHashMap<>();
          body.put("type", "about:blank");
          body.put("title", "Unauthorized");
          body.put("status", HttpStatus.UNAUTHORIZED.value());
          body.put("detail", "A valid bearer token is required.");
          body.put("instance", request.getRequestURI());
          response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
          json.writeValue(response.getOutputStream(), body);
        };
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> {})
                    .authenticationEntryPoint(problem));
    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${tasks.jwt.secret}") String secret) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException("TASKS_JWT_SECRET must be at least 32 bytes");
    }
    SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }
}

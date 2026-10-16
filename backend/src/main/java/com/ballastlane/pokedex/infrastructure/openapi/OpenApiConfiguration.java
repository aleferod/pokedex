package com.ballastlane.pokedex.infrastructure.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadata of the published OpenAPI document (Swagger UI at /swagger-ui.html). */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

  /** Name operations reference in {@code @SecurityRequirement}. */
  public static final String BEARER_JWT = "bearer-jwt";

  @Bean
  OpenAPI pokedexOpenApi(ObjectProvider<BuildProperties> buildProperties) {
    BuildProperties build = buildProperties.getIfAvailable();
    return new OpenAPI()
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_JWT,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token from POST /api/v1/auth/login")))
        .info(
            new Info()
                .title("Pokédex API")
                .version(build != null ? build.getVersion() : "dev")
                .description(
                    "Browse Pokémon from PokeAPI, keep a local replica with proprietary fields and"
                        + " manage it. Errors follow RFC 7807; see docs/problems.md.")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
  }
}

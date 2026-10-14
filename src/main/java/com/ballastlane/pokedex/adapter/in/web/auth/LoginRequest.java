package com.ballastlane.pokedex.adapter.in.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/v1/auth/login}. */
public record LoginRequest(
    @Schema(example = "ash") @NotBlank @Size(max = 100) String username,
    @Schema(example = "Pikachu@25") @NotBlank @Size(max = 200) String password) {

  @Override
  public String toString() {
    return "LoginRequest[username=" + username + ", password=***]";
  }
}

package com.ballastlane.pokedex.adapter.in.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/auth/register}. Bean Validation gives fast, per-field feedback and
 * caps input sizes; the domain value objects and password policy remain the authority on the actual
 * rules. Unknown members (such as {@code role}) are rejected by Jackson.
 */
public record RegisterRequest(
    @Schema(
            example = "ash",
            description = "3-30 letters, digits, '.', '_' or '-'; case-insensitive")
        @NotBlank
        @Size(max = 100)
        String username,
    @Schema(example = "ash@pallet.town") @NotBlank @Size(max = 254) String email,
    @Schema(
            example = "Pikachu@25",
            description =
                "8-72 characters with upper case, lower case, digit and special character")
        @NotBlank
        @Size(max = 200)
        String password) {

  @Override
  public String toString() {
    return "RegisterRequest[username=" + username + ", email=" + email + ", password=***]";
  }
}

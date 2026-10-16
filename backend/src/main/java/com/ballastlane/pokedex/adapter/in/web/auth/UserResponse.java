package com.ballastlane.pokedex.adapter.in.web.auth;

import java.time.Instant;
import java.util.UUID;

/** Public view of an account: never includes the password or its hash. */
public record UserResponse(
    UUID id, String username, String email, String role, Instant createdAt) {}

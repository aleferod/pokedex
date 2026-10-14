package com.ballastlane.pokedex.adapter.in.web.auth;

import java.time.Instant;

/** Result of a login: send {@code Authorization: Bearer <accessToken>} on protected requests. */
public record TokenResponse(
    String accessToken, String tokenType, Instant expiresAt, UserResponse user) {}

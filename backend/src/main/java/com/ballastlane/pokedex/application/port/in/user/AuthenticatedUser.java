package com.ballastlane.pokedex.application.port.in.user;

import com.ballastlane.pokedex.application.port.out.user.IssuedToken;
import com.ballastlane.pokedex.domain.user.User;

/** Result of a successful login: who logged in and their access token. */
public record AuthenticatedUser(User user, IssuedToken token) {}

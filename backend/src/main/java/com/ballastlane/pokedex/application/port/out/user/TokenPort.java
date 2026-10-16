package com.ballastlane.pokedex.application.port.out.user;

import com.ballastlane.pokedex.domain.user.User;

/** Issues access tokens identifying an account and its role. */
public interface TokenPort {

  IssuedToken issue(User user);
}

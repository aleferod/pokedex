package com.ballastlane.pokedex.domain.user;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.UUID;

/** Value object: the identity of an account. */
public record UserId(UUID value) {

  public UserId {
    if (value == null) {
      throw new InvalidFieldException("id", "is required");
    }
  }

  public static UserId random() {
    return new UserId(UUID.randomUUID());
  }

  public static UserId of(String value) {
    try {
      return new UserId(UUID.fromString(value));
    } catch (IllegalArgumentException | NullPointerException notAUuid) {
      throw new InvalidFieldException("id", "must be a UUID");
    }
  }
}

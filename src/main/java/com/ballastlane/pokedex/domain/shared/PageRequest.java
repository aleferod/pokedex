package com.ballastlane.pokedex.domain.shared;

/** Value object: which page of a listing is wanted. {@code page} is zero-based. */
public record PageRequest(int page, int size) {

  public static final int MAX_SIZE = 100;
  public static final int DEFAULT_SIZE = 20;

  public PageRequest {
    if (page < 0) {
      throw new InvalidFieldException("page", "must be 0 or greater");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new InvalidFieldException("size", "must be between 1 and " + MAX_SIZE);
    }
  }

  /** Index of the first element of this page (as a long, so large pages cannot overflow). */
  public long offset() {
    return (long) page * size;
  }
}

package com.ballastlane.pokedex.adapter.in.web.error;

/**
 * The request must be conditional (RFC 6585, 428): an unsafe method without the {@code If-Match}
 * header it needs to prevent lost updates.
 */
public class PreconditionRequiredException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String header;

  public PreconditionRequiredException(String header) {
    super(
        "This request must include an "
            + header
            + " header with the ETag of the version you last read.");
    this.header = header;
  }

  public String header() {
    return header;
  }
}

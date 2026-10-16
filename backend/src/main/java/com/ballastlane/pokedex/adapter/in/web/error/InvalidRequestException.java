package com.ballastlane.pokedex.adapter.in.web.error;

import java.util.List;

/**
 * The HTTP request itself is malformed (a header or body member the endpoint reads by hand), as
 * opposed to a domain rule being broken. Mapped to 400 with every entry.
 */
public class InvalidRequestException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<ProblemFieldError> errors;

  public InvalidRequestException(String detail, List<ProblemFieldError> errors) {
    super(detail);
    this.errors = List.copyOf(errors);
  }

  public List<ProblemFieldError> errors() {
    return errors;
  }
}

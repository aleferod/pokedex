package com.ballastlane.pokedex.adapter.in.web.error;

/**
 * The problem types this API returns. Each slug is an anchor in {@code docs/problems.md}, so every
 * {@code type} URI resolves to documentation (enforced by {@code ProblemTypesDocumentedTest}).
 * Titles never vary between occurrences of the same type, as RFC 7807 recommends.
 */
public enum ProblemType {
  BAD_REQUEST("bad-request", "Bad request", 400),
  UNAUTHORIZED("unauthorized", "Authentication required", 401),
  FORBIDDEN("forbidden", "Access denied", 403),
  NOT_FOUND("not-found", "Resource not found", 404),
  METHOD_NOT_ALLOWED("method-not-allowed", "Method not allowed", 405),
  NOT_ACCEPTABLE("not-acceptable", "Not acceptable", 406),
  CONFLICT("conflict", "Conflict", 409),
  PRECONDITION_FAILED("precondition-failed", "Precondition failed", 412),
  PAYLOAD_TOO_LARGE("payload-too-large", "Payload too large", 413),
  UNSUPPORTED_MEDIA_TYPE("unsupported-media-type", "Unsupported media type", 415),
  PRECONDITION_REQUIRED("precondition-required", "Precondition required", 428),
  INTERNAL_ERROR("internal-error", "Internal server error", 500),
  SERVICE_UNAVAILABLE("service-unavailable", "Service unavailable", 503),
  /** Any other 4xx status. */
  CLIENT_ERROR("client-error", "Client error", 400),
  /** Any other 5xx status. */
  SERVER_ERROR("server-error", "Server error", 500);

  private final String slug;
  private final String title;
  private final int status;

  ProblemType(String slug, String title, int status) {
    this.slug = slug;
    this.title = title;
    this.status = status;
  }

  public String slug() {
    return slug;
  }

  public String title() {
    return title;
  }

  public int status() {
    return status;
  }

  /** The specific type for an HTTP status, or the client/server fallback type. */
  public static ProblemType forStatus(int status) {
    for (ProblemType type : values()) {
      if (type.status == status && type != CLIENT_ERROR && type != SERVER_ERROR) {
        return type;
      }
    }
    return status >= 500 ? SERVER_ERROR : CLIENT_ERROR;
  }
}

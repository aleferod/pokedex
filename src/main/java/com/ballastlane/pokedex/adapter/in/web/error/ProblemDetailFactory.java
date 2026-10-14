package com.ballastlane.pokedex.adapter.in.web.error;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * Factory for the API's RFC 7807 problem details: the only place that decides {@code type}, {@code
 * title}, {@code instance}, {@code timestamp} and {@code errors}, so every error response has
 * exactly the same members whether it comes from our handlers, Spring MVC or the servlet container.
 */
@Component
public class ProblemDetailFactory {

  static final String TIMESTAMP = "timestamp";
  static final String ERRORS = "errors";

  private final String typeBaseUri;
  private final Clock clock;

  public ProblemDetailFactory(@Value("${app.problems.base-uri}") String typeBaseUri, Clock clock) {
    this.typeBaseUri = typeBaseUri;
    this.clock = clock;
  }

  /** A problem of a known type with a caller-safe detail message. */
  public ProblemDetail create(ProblemType type, String detail, String path) {
    return create(type, detail, path, List.of());
  }

  /** A problem of a known type carrying field-level errors. */
  public ProblemDetail create(
      ProblemType type, String detail, String path, List<ProblemFieldError> errors) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(type.status()), detail);
    return complete(problem, type, path, errors);
  }

  /** A problem for an arbitrary HTTP status, typed by {@link ProblemType#forStatus(int)}. */
  public ProblemDetail forStatus(int status, String detail, String path) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
    return complete(problem, ProblemType.forStatus(status), path, List.of());
  }

  /** Completes a problem Spring MVC already built (status and detail set) with our members. */
  public ProblemDetail complete(ProblemDetail problem, String path) {
    ProblemType type = ProblemType.forStatus(problem.getStatus());
    return complete(problem, type, path, List.of());
  }

  private ProblemDetail complete(
      ProblemDetail problem, ProblemType type, String path, List<ProblemFieldError> errors) {
    problem.setType(URI.create(typeBaseUri + type.slug()));
    problem.setTitle(type.title());
    if (problem.getDetail() == null || problem.getDetail().isBlank()) {
      problem.setDetail(type.title() + ".");
    }
    problem.setInstance(toUri(path));
    Map<String, Object> members = new LinkedHashMap<>();
    members.put(TIMESTAMP, Instant.now(clock));
    members.put(ERRORS, List.copyOf(errors));
    problem.setProperties(members);
    return problem;
  }

  private static URI toUri(String path) {
    try {
      return URI.create(path);
    } catch (IllegalArgumentException notAValidUri) {
      return URI.create(UriUtils.encodePath(path, StandardCharsets.UTF_8));
    }
  }
}

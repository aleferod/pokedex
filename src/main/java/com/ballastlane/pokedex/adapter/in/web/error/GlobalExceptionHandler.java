package com.ballastlane.pokedex.adapter.in.web.error;

import com.ballastlane.pokedex.adapter.in.web.correlation.CorrelationIdFilter;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.user.DuplicateUserException;
import com.ballastlane.pokedex.domain.user.InvalidCredentialsException;
import com.ballastlane.pokedex.domain.user.UserNotFoundException;
import com.ballastlane.pokedex.domain.user.password.WeakPasswordException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Single error translator for the API (RFC 7807).
 *
 * <p>Template Method: {@link ResponseEntityExceptionHandler} already maps every Spring MVC
 * exception (404, 405, 415, 400, ...) to a {@link ProblemDetail}; we override its final hook {@link
 * #createResponseEntity} so each of those problems is completed with our members, and two of its
 * steps ({@link #handleMethodArgumentNotValid}, {@link #handleHttpMessageNotReadable}) to name the
 * offending fields. Domain exceptions are mapped to statuses here, so the domain stays HTTP-free.
 * Anything unexpected becomes a generic 500 that leaks nothing and is logged with its stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String INVALID_FIELDS = "The request has invalid fields.";
  private static final String BEARER = "Bearer";

  private final ProblemDetailFactory problems;

  public GlobalExceptionHandler(ProblemDetailFactory problems) {
    this.problems = problems;
  }

  // ---- input errors (400) ---------------------------------------------------------------------

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<ProblemFieldError> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(error -> new ProblemFieldError(error.getField(), error.getDefaultMessage()))
            .toList();
    ProblemDetail problem =
        problems.create(ProblemType.BAD_REQUEST, INVALID_FIELDS, path(request), errors);
    return handleExceptionInternal(exception, problem, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Throwable cause = exception.getMostSpecificCause();
    ProblemDetail problem;
    if (cause instanceof UnrecognizedPropertyException unknown) {
      problem =
          problems.create(
              ProblemType.BAD_REQUEST,
              "The request body contains members this endpoint does not accept.",
              path(request),
              List.of(new ProblemFieldError(fieldPath(unknown), "is not an accepted field")));
    } else if (cause instanceof MismatchedInputException mismatch
        && !mismatch.getPath().isEmpty()) {
      problem =
          problems.create(
              ProblemType.BAD_REQUEST,
              "The request body has values of the wrong type.",
              path(request),
              List.of(new ProblemFieldError(fieldPath(mismatch), "has the wrong type")));
    } else {
      problem =
          problems.create(ProblemType.BAD_REQUEST, "Malformed JSON request body.", path(request));
    }
    return handleExceptionInternal(exception, problem, headers, status, request);
  }

  @ExceptionHandler(InvalidFieldException.class)
  ResponseEntity<Object> handleInvalidField(InvalidFieldException exception, WebRequest request) {
    return respond(
        problems.create(
            ProblemType.BAD_REQUEST,
            INVALID_FIELDS,
            path(request),
            List.of(new ProblemFieldError(exception.field(), exception.reason()))));
  }

  @ExceptionHandler(WeakPasswordException.class)
  ResponseEntity<Object> handleWeakPassword(WeakPasswordException exception, WebRequest request) {
    List<ProblemFieldError> errors =
        exception.violations().stream()
            .map(violation -> new ProblemFieldError("password", violation))
            .toList();
    return respond(
        problems.create(
            ProblemType.BAD_REQUEST, "The password is too weak.", path(request), errors));
  }

  // ---- conflicts (409) ------------------------------------------------------------------------

  @ExceptionHandler(DuplicateUserException.class)
  ResponseEntity<Object> handleDuplicateUser(DuplicateUserException exception, WebRequest request) {
    return respond(
        problems.create(
            ProblemType.CONFLICT,
            "An account with this " + exception.field() + " already exists.",
            path(request),
            List.of(new ProblemFieldError(exception.field(), "is already taken"))));
  }

  // ---- authentication (401) -------------------------------------------------------------------

  @ExceptionHandler(InvalidCredentialsException.class)
  ResponseEntity<Object> handleInvalidCredentials(
      InvalidCredentialsException exception, WebRequest request) {
    return unauthorized(exception.getMessage(), request);
  }

  @ExceptionHandler(UserNotFoundException.class)
  ResponseEntity<Object> handleUserNotFound(UserNotFoundException exception, WebRequest request) {
    return unauthorized("The account for this token no longer exists.", request);
  }

  // ---- everything else (500) ------------------------------------------------------------------

  @ExceptionHandler(Exception.class)
  ResponseEntity<Object> handleUnexpected(Exception exception, HttpServletRequest request) {
    LOG.error(
        "Unexpected error while handling {} {}",
        request.getMethod(),
        request.getRequestURI(),
        exception);
    ProblemDetail problem =
        problems.create(
            ProblemType.INTERNAL_ERROR,
            internalErrorDetail(MDC.get(CorrelationIdFilter.MDC_KEY)),
            request.getRequestURI());
    return ResponseEntity.internalServerError().body(problem);
  }

  @Override
  protected ResponseEntity<Object> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    ProblemDetail problem =
        body instanceof ProblemDetail built ? built : ProblemDetail.forStatus(statusCode);
    return super.createResponseEntity(
        problems.complete(problem, path(request)), headers, statusCode, request);
  }

  static String internalErrorDetail(String correlationId) {
    return "An unexpected error occurred. If it persists, report it quoting correlation id "
        + correlationId
        + ".";
  }

  private ResponseEntity<Object> unauthorized(String detail, WebRequest request) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .header(HttpHeaders.WWW_AUTHENTICATE, BEARER)
        .body(problems.create(ProblemType.UNAUTHORIZED, detail, path(request)));
  }

  private static ResponseEntity<Object> respond(ProblemDetail problem) {
    return ResponseEntity.status(problem.getStatus()).body(problem);
  }

  /** JSON path of the offending member, e.g. {@code username} or {@code tags[2]}. */
  private static String fieldPath(JacksonException exception) {
    StringBuilder path = new StringBuilder();
    for (JacksonException.Reference reference : exception.getPath()) {
      if (reference.getPropertyName() != null) {
        if (!path.isEmpty()) {
          path.append('.');
        }
        path.append(reference.getPropertyName());
      } else if (reference.getIndex() >= 0) {
        path.append('[').append(reference.getIndex()).append(']');
      }
    }
    return path.isEmpty() ? "body" : path.toString();
  }

  private static String path(WebRequest request) {
    if (request instanceof ServletWebRequest servletRequest) {
      return servletRequest.getRequest().getRequestURI();
    }
    return "/";
  }
}

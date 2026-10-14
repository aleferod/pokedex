package com.ballastlane.pokedex.adapter.in.web.error;

import com.ballastlane.pokedex.adapter.in.web.correlation.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Single error translator for the API (RFC 7807).
 *
 * <p>Template Method: {@link ResponseEntityExceptionHandler} already maps every Spring MVC
 * exception (404, 405, 415, 400, ...) to a {@link ProblemDetail}; we override its final hook {@link
 * #createResponseEntity} so each of those problems is completed with our members. Anything
 * unexpected becomes a generic 500 that leaks nothing and is logged with its stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final ProblemDetailFactory problems;

  public GlobalExceptionHandler(ProblemDetailFactory problems) {
    this.problems = problems;
  }

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

  private static String path(WebRequest request) {
    if (request instanceof ServletWebRequest servletRequest) {
      return servletRequest.getRequest().getRequestURI();
    }
    return "/";
  }
}

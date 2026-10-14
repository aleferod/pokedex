package com.ballastlane.pokedex.adapter.in.web.error;

import com.ballastlane.pokedex.adapter.in.web.correlation.CorrelationIdFilter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces Spring Boot's {@code BasicErrorController}: errors that never reach a request handler
 * (container rejections, failures inside filters) are forwarded here by the servlet container and
 * answered with the same RFC 7807 contract as everything else. Container messages and exceptions
 * are logged, never returned.
 */
@RestController
@RequestMapping("${server.error.path:/error}")
public class ProblemDetailErrorController implements ErrorController {

  private static final Logger LOG = LoggerFactory.getLogger(ProblemDetailErrorController.class);

  private final ProblemDetailFactory problems;

  public ProblemDetailErrorController(ProblemDetailFactory problems) {
    this.problems = problems;
  }

  @RequestMapping
  ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
    if (!(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) instanceof Integer status)) {
      // Called directly by a client rather than forwarded by the container.
      return respond(
          problems.create(
              ProblemType.NOT_FOUND,
              "No endpoint at " + request.getRequestURI() + ".",
              request.getRequestURI()));
    }
    String path =
        request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String uri
            ? uri
            : request.getRequestURI();
    if (status >= 500) {
      LOG.error(
          "Request {} failed outside the MVC handlers with status {}",
          path,
          status,
          request.getAttribute(RequestDispatcher.ERROR_EXCEPTION));
      return respond(
          problems.forStatus(
              status,
              GlobalExceptionHandler.internalErrorDetail(MDC.get(CorrelationIdFilter.MDC_KEY)),
              path));
    }
    LOG.warn(
        "Request {} rejected before reaching a handler with status {}: {}",
        path,
        status,
        request.getAttribute(RequestDispatcher.ERROR_MESSAGE));
    ProblemType type = ProblemType.forStatus(status);
    return respond(
        problems.forStatus(status, "The request was rejected: " + type.title() + ".", path));
  }

  private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
    return ResponseEntity.status(problem.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}

package com.example.tasks.web;

import com.example.tasks.task.InvalidDueDateException;
import com.example.tasks.task.StaleTaskException;
import com.example.tasks.task.TaskNotFoundException;
import com.example.tasks.task.UnknownUserException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(TaskNotFoundException.class)
  ProblemDetail notFound(TaskNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler({StaleTaskException.class, ObjectOptimisticLockingFailureException.class})
  ProblemDetail conflict(RuntimeException e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "The task was modified by another request; reload it and retry.");
  }

  @ExceptionHandler(InvalidDueDateException.class)
  ProblemDetail invalidDueDate(InvalidDueDateException e) {
    return withErrors(
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()),
        List.of(error("dueDate", "must not be in the past")));
  }

  @ExceptionHandler(UnknownUserException.class)
  ResponseEntity<ProblemDetail> unknownUser(UnknownUserException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"")
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Unknown user"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ProblemDetail typeMismatch(MethodArgumentTypeMismatchException e) {
    return withErrors(
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid parameter"),
        List.of(error(e.getName(), "has an invalid value")));
  }

  /** Anything unexpected: logged with its stack trace, answered without internals. */
  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception e, HttpServletRequest request) {
    LOG.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> error(fieldError.getField(), fieldError.getDefaultMessage()))
            .toList();
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    return ResponseEntity.badRequest().body(withErrors(problem, errors));
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var errors =
        ex.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                error(
                                    result.getMethodParameter().getParameterName(),
                                    error.getDefaultMessage())))
            .toList();
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    return ResponseEntity.badRequest().body(withErrors(problem, errors));
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
    Throwable cause = ex.getCause();
    if (cause instanceof UnrecognizedPropertyException unknown) {
      withErrors(problem, List.of(error(unknown.getPropertyName(), "is not a known field")));
    } else if (cause instanceof InvalidFormatException invalid && !invalid.getPath().isEmpty()) {
      withErrors(
          problem,
          List.of(error(invalid.getPath().getLast().getPropertyName(), "has an invalid value")));
    }
    return ResponseEntity.badRequest().body(problem);
  }

  private static ProblemDetail withErrors(ProblemDetail problem, List<Map<String, String>> errors) {
    problem.setProperty("errors", errors);
    return problem;
  }

  private static Map<String, String> error(String field, String message) {
    return Map.of("field", field, "message", message);
  }
}

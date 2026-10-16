package com.ballastlane.pokedex.adapter.in.web.security;

import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailFactory;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailWriter;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 401 for protected routes: Spring's {@link BearerTokenAuthenticationEntryPoint} sets the RFC 6750
 * {@code WWW-Authenticate} challenge (with {@code error="invalid_token"} for bad tokens), then the
 * body is written as an {@code unauthorized} problem detail like every other error.
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final AuthenticationEntryPoint challenge = new BearerTokenAuthenticationEntryPoint();
  private final ProblemDetailFactory problems;
  private final ProblemDetailWriter writer;

  public ProblemDetailAuthenticationEntryPoint(
      ProblemDetailFactory problems, ProblemDetailWriter writer) {
    this.problems = problems;
    this.writer = writer;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    challenge.commence(request, response, authException);
    String detail =
        authException instanceof InvalidBearerTokenException
            ? "The access token is invalid or has expired."
            : "A valid bearer token is required to access this resource.";
    writer.write(
        problems.create(ProblemType.UNAUTHORIZED, detail, request.getRequestURI()), response);
  }
}

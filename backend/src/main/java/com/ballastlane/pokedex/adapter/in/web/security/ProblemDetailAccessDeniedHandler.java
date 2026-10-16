package com.ballastlane.pokedex.adapter.in.web.security;

import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailFactory;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailWriter;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 403 for authenticated callers without the required role, as a {@code forbidden} problem. */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

  private final AccessDeniedHandler challenge = new BearerTokenAccessDeniedHandler();
  private final ProblemDetailFactory problems;
  private final ProblemDetailWriter writer;

  public ProblemDetailAccessDeniedHandler(
      ProblemDetailFactory problems, ProblemDetailWriter writer) {
    this.problems = problems;
    this.writer = writer;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    challenge.handle(request, response, accessDeniedException);
    writer.write(
        problems.create(
            ProblemType.FORBIDDEN,
            "Your role does not allow this operation.",
            request.getRequestURI()),
        response);
  }
}

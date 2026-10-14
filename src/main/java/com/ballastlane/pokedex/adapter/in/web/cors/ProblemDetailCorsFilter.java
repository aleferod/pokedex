package com.ballastlane.pokedex.adapter.in.web.cors;

import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailFactory;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemDetailWriter;
import com.ballastlane.pokedex.adapter.in.web.error.ProblemType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Same flow as Spring's {@code CorsFilter} (answer valid preflights, decorate allowed requests),
 * but a rejected cross-origin request gets a 403 problem detail instead of Spring's plain-text
 * "Invalid CORS request", keeping the error contract uniform.
 */
public class ProblemDetailCorsFilter extends OncePerRequestFilter {

  private final CorsConfigurationSource configurationSource;
  private final ProblemDetailFactory problems;
  private final ProblemDetailWriter writer;
  private final CorsProcessor processor = new BodylessRejectionCorsProcessor();

  public ProblemDetailCorsFilter(
      CorsConfigurationSource configurationSource,
      ProblemDetailFactory problems,
      ProblemDetailWriter writer) {
    this.configurationSource = configurationSource;
    this.problems = problems;
    this.writer = writer;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    CorsConfiguration configuration = configurationSource.getCorsConfiguration(request);
    if (!processor.processRequest(configuration, request, response)) {
      writer.write(
          problems.create(
              ProblemType.FORBIDDEN,
              "Cross-origin requests from this origin are not allowed.",
              request.getRequestURI()),
          response);
      return;
    }
    if (CorsUtils.isPreFlightRequest(request)) {
      return;
    }
    chain.doFilter(request, response);
  }

  /** Leaves the rejection body to the filter. */
  private static final class BodylessRejectionCorsProcessor extends DefaultCorsProcessor {

    @Override
    protected void rejectRequest(ServerHttpResponse response) {
      response.setStatusCode(HttpStatus.FORBIDDEN);
    }
  }
}

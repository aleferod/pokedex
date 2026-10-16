package com.ballastlane.pokedex.adapter.in.web.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First link of the servlet filter chain (Chain of Responsibility): gives every request a
 * correlation id, returns it in the {@value #HEADER} response header and exposes it to logging
 * through the MDC key {@value #MDC_KEY}.
 *
 * <p>A caller-supplied id is trusted only if it is 1-64 ASCII letters, digits or hyphens; anything
 * else is replaced, so callers cannot inject content into log lines. The header is written before
 * the chain runs, so error responses carry it too, and the id is kept on the request so the
 * container's error dispatch reuses it instead of generating a second one.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Correlation-Id";
  public static final String MDC_KEY = "correlationId";

  private static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".id";
  private static final Pattern TRUSTED_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String correlationId = resolve(request);
    request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
    response.setHeader(HEADER, correlationId);
    MDC.put(MDC_KEY, correlationId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  @Override
  protected boolean shouldNotFilterErrorDispatch() {
    return false;
  }

  private static String resolve(HttpServletRequest request) {
    if (request.getAttribute(REQUEST_ATTRIBUTE) instanceof String alreadyAssigned) {
      return alreadyAssigned;
    }
    String supplied = request.getHeader(HEADER);
    if (supplied != null && TRUSTED_ID.matcher(supplied).matches()) {
      return supplied;
    }
    return UUID.randomUUID().toString();
  }
}

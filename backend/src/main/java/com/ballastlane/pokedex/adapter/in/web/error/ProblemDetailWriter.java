package com.ballastlane.pokedex.adapter.in.web.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes a problem detail straight to the servlet response, for errors raised in filters (CORS
 * rejections, and later authentication/authorization failures) where Spring MVC's message
 * converters are not involved. Produces the same flat JSON members as MVC responses.
 */
@Component
public class ProblemDetailWriter {

  private final JsonMapper jsonMapper;

  public ProblemDetailWriter(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  public void write(ProblemDetail problem, HttpServletResponse response) throws IOException {
    response.setStatus(problem.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    // JSON is UTF-8 by definition (RFC 8259); Jackson writes UTF-8 bytes to the stream.
    jsonMapper.writeValue(response.getOutputStream(), members(problem));
  }

  private static Map<String, Object> members(ProblemDetail problem) {
    Map<String, Object> members = new LinkedHashMap<>();
    members.put("type", String.valueOf(problem.getType()));
    members.put("title", problem.getTitle());
    members.put("status", problem.getStatus());
    members.put("detail", problem.getDetail());
    members.put("instance", String.valueOf(problem.getInstance()));
    if (problem.getProperties() != null) {
      members.putAll(problem.getProperties());
    }
    return members;
  }
}

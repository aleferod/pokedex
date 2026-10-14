package com.ballastlane.pokedex.adapter.in.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.in.web.correlation.CorrelationIdFilter;
import com.ballastlane.pokedex.support.WebSliceTest;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Errors raised outside Spring MVC (container rejections, filter failures) are forwarded by the
 * servlet container to {@code /error}; they must use the same problem-detail contract.
 */
@WebSliceTest
@ExtendWith(OutputCaptureExtension.class)
class ProblemDetailErrorControllerTest {

  @Autowired private MockMvcTester mvc;

  @Test
  void should_returnProblemDetail_when_containerForwardsClientError() {
    // When the container forwards a request it rejected with 400
    MvcTestResult result =
        mvc.get()
            .uri("/error")
            .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
            .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/rejected")
            .requestAttr(
                RequestDispatcher.ERROR_MESSAGE, "Invalid character found in the request target")
            .exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result)
        .bodyJson()
        .extractingPath("$")
        .asMap()
        .containsOnlyKeys("type", "title", "status", "detail", "instance", "timestamp", "errors");
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#bad-request");
    assertThat(result).bodyJson().extractingPath("$.instance").isEqualTo("/api/v1/rejected");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.detail")
        .asString()
        .doesNotContain("Invalid character");
  }

  @Test
  void should_returnGenericProblem500_when_containerForwardsException(CapturedOutput output)
      throws Exception {
    // When a filter failed with an exception the container caught
    MvcTestResult result =
        mvc.get()
            .uri("/error")
            .header(CorrelationIdFilter.HEADER, "demo-filter-500")
            .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
            .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/broken")
            .requestAttr(
                RequestDispatcher.ERROR_EXCEPTION,
                new IllegalStateException("secret-filter-detail"))
            .exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#internal-error");
    assertThat(result).bodyJson().extractingPath("$.detail").asString().contains("demo-filter-500");
    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("secret-filter-detail")
        .doesNotContain("IllegalStateException");
    assertThat(output.getAll())
        .contains("java.lang.IllegalStateException: secret-filter-detail")
        .contains("demo-filter-500");
  }

  @Test
  void should_return404_when_errorPathIsRequestedDirectly() {
    // When a client calls the error path itself (no container error attributes)
    MvcTestResult result = mvc.get().uri("/error").exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#not-found");
  }
}

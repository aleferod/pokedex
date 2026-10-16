package com.ballastlane.pokedex.adapter.in.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.in.web.correlation.CorrelationIdFilter;
import com.ballastlane.pokedex.support.WebSliceTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebSliceTest
@Import(GlobalExceptionHandlerTest.ProbeController.class)
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

  private static final String SECRET = "secret-detail jdbc:postgresql://db:5432/pokedex";

  @Autowired private MockMvcTester mvc;

  @Test
  void should_returnProblemDetail404_when_routeIsUnknown() {
    // When
    MvcTestResult result =
        mvc.get()
            .uri("/api/v1/does-not-exist")
            .header(CorrelationIdFilter.HEADER, "demo-404")
            .exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .hasHeader(CorrelationIdFilter.HEADER, "demo-404");
    assertThat(result)
        .bodyJson()
        .extractingPath("$")
        .asMap()
        .containsOnlyKeys("type", "title", "status", "detail", "instance", "timestamp", "errors");
    assertThat(result).bodyJson().extractingPath("$.status").asNumber().isEqualTo(404);
    assertThat(result).bodyJson().extractingPath("$.instance").isEqualTo("/api/v1/does-not-exist");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.type")
        .asString()
        .endsWith("problems.md#not-found");
    assertThat(result).bodyJson().extractingPath("$.title").asString().isNotBlank();
    assertThat(result).bodyJson().extractingPath("$.detail").asString().isNotBlank();
    assertThat(result).bodyJson().extractingPath("$.errors").asArray().isEmpty();
    assertThat(result)
        .bodyJson()
        .extractingPath("$.timestamp")
        .asString()
        .satisfies(timestamp -> assertThat(Instant.parse(timestamp)).isNotNull());
  }

  @Test
  void should_return404_when_unknownRouteIsCalledWithAnyMethod() {
    // When
    MvcTestResult result = mvc.post().uri("/api/v1/does-not-exist").exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void should_returnProblemDetail405WithAllowHeader_when_methodIsNotSupported() {
    // When
    MvcTestResult result = mvc.delete().uri("/test/ok").exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .headers()
        .hasHeaderSatisfying(
            HttpHeaders.ALLOW, values -> assertThat(values.toString()).contains("GET"));
    assertThat(result)
        .bodyJson()
        .extractingPath("$.type")
        .asString()
        .endsWith("#method-not-allowed");
    assertThat(result).bodyJson().extractingPath("$.status").asNumber().isEqualTo(405);
  }

  @Test
  void should_returnProblemDetail400_when_parameterHasWrongType() {
    // When
    MvcTestResult result = mvc.get().uri("/test/typed?count=abc").exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#bad-request");
  }

  @Test
  void should_returnGenericProblem500WithoutInternals_when_unexpectedErrorOccurs()
      throws Exception {
    // When
    MvcTestResult result =
        mvc.get().uri("/test/boom").header(CorrelationIdFilter.HEADER, "demo-500").exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .hasHeader(CorrelationIdFilter.HEADER, "demo-500");
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#internal-error");
    assertThat(result).bodyJson().extractingPath("$.detail").asString().contains("demo-500");
    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("IllegalStateException")
        .doesNotContain("secret-detail")
        .doesNotContain("jdbc:")
        .doesNotContain("at com.ballastlane")
        .doesNotContain("trace");
  }

  @Test
  void should_logErrorWithStackTraceAndCorrelationId_when_unexpectedErrorOccurs(
      CapturedOutput output) {
    // When
    mvc.get().uri("/test/boom").header(CorrelationIdFilter.HEADER, "demo-500-log").exchange();

    // Then
    assertThat(output.getAll().lines().filter(line -> line.contains(" ERROR ")))
        .anySatisfy(line -> assertThat(line).contains("demo-500-log"));
    assertThat(output.getAll())
        .contains("java.lang.IllegalStateException: " + SECRET)
        .contains(
            "at com.ballastlane.pokedex.adapter.in.web.error.GlobalExceptionHandlerTest$ProbeController.boom");
  }

  @RestController
  static class ProbeController {

    @GetMapping("/test/ok")
    String ok() {
      return "ok";
    }

    @GetMapping("/test/typed")
    String typed(@RequestParam int count) {
      return "count=" + count;
    }

    @GetMapping("/test/boom")
    String boom() {
      throw new IllegalStateException(SECRET);
    }
  }
}

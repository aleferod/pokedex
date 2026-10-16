package com.ballastlane.pokedex.adapter.in.web.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.support.WebSliceTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebSliceTest
@Import(CorrelationIdLoggingTest.LoggingController.class)
@ExtendWith(OutputCaptureExtension.class)
class CorrelationIdLoggingTest {

  @Autowired private MockMvcTester mvc;

  @Test
  void should_includeCorrelationId_when_logWrittenDuringRequest(CapturedOutput output) {
    // When
    mvc.get()
        .uri("/test/log?marker=during-request")
        .header(CorrelationIdFilter.HEADER, "demo-log")
        .exchange();

    // Then
    assertThat(linesContaining(output, "marker=during-request"))
        .singleElement()
        .asString()
        .contains("demo-log");
  }

  @Test
  void should_neverLogRejectedId_when_idIsUntrusted(CapturedOutput output) {
    // When
    mvc.get()
        .uri("/test/log?marker=untrusted")
        .header(CorrelationIdFilter.HEADER, "abc\r\nINJECTED-LINE")
        .exchange();

    // Then
    assertThat(linesContaining(output, "marker=untrusted")).hasSize(1);
    assertThat(output.getAll()).doesNotContain("INJECTED-LINE");
  }

  @Test
  void should_notLeakPreviousId_when_nextRequestRunsOnSameThread(CapturedOutput output) {
    // Given a first request handled on this (MockMvc) thread
    mvc.get()
        .uri("/test/log?marker=first")
        .header(CorrelationIdFilter.HEADER, "first-request-id")
        .exchange();

    // When a second request without id runs on the same thread
    mvc.get().uri("/test/log?marker=second").exchange();

    // Then
    assertThat(linesContaining(output, "marker=first"))
        .singleElement()
        .asString()
        .contains("first-request-id");
    assertThat(linesContaining(output, "marker=second"))
        .singleElement()
        .asString()
        .doesNotContain("first-request-id");
  }

  private static List<String> linesContaining(CapturedOutput output, String text) {
    return output.getAll().lines().filter(line -> line.contains(text)).toList();
  }

  @RestController
  static class LoggingController {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingController.class);

    @GetMapping("/test/log")
    String log(@RequestParam String marker) {
      LOG.info("handling request marker={}", marker);
      return "logged";
    }
  }
}

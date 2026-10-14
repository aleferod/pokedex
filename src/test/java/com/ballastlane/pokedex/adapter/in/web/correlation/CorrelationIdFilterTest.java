package com.ballastlane.pokedex.adapter.in.web.correlation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();
  private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/x");
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void should_echoId_when_requestCarriesValidId() throws Exception {
    // Given
    request.addHeader(CorrelationIdFilter.HEADER, "demo-123");

    // When
    filter.doFilter(request, response, (req, res) -> {});

    // Then
    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("demo-123");
  }

  @Test
  void should_acceptId_when_idIsExactly64Characters() throws Exception {
    // Given
    String maxLength = "a".repeat(64);
    request.addHeader(CorrelationIdFilter.HEADER, maxLength);

    // When
    filter.doFilter(request, response, (req, res) -> {});

    // Then
    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(maxLength);
  }

  @Test
  void should_generateUuid_when_requestHasNoId() throws Exception {
    // When
    filter.doFilter(request, response, (req, res) -> {});

    // Then
    assertThat(response.getHeader(CorrelationIdFilter.HEADER))
        .satisfies(CorrelationIdFilterTest::isUuid);
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc\r\nINJECTED", "<script>", " ", "abc def", "ação", "id;drop"})
  void should_replaceWithUuid_when_idContainsIllegalCharacters(String untrusted) throws Exception {
    // Given
    request.addHeader(CorrelationIdFilter.HEADER, untrusted);

    // When
    filter.doFilter(request, response, (req, res) -> {});

    // Then
    assertThat(response.getHeader(CorrelationIdFilter.HEADER))
        .isNotEqualTo(untrusted)
        .satisfies(CorrelationIdFilterTest::isUuid);
  }

  @Test
  void should_replaceWithUuid_when_idIsLongerThan64Characters() throws Exception {
    // Given
    request.addHeader(CorrelationIdFilter.HEADER, "a".repeat(65));

    // When
    filter.doFilter(request, response, (req, res) -> {});

    // Then
    assertThat(response.getHeader(CorrelationIdFilter.HEADER))
        .satisfies(CorrelationIdFilterTest::isUuid);
  }

  @Test
  void should_exposeIdInMdc_when_chainHandlesTheRequest() throws Exception {
    // Given
    request.addHeader(CorrelationIdFilter.HEADER, "demo-mdc");
    AtomicReference<String> seenByChain = new AtomicReference<>();
    FilterChain chain = (req, res) -> seenByChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));

    // When
    filter.doFilter(request, response, chain);

    // Then
    assertThat(seenByChain).hasValue("demo-mdc");
    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void should_setHeaderAndClearMdc_when_chainThrows() {
    // Given
    request.addHeader(CorrelationIdFilter.HEADER, "demo-boom");
    FilterChain failingChain =
        (req, res) -> {
          throw new IllegalStateException("boom");
        };

    // When / Then
    assertThatThrownBy(() -> filter.doFilter(request, response, failingChain))
        .isInstanceOf(IllegalStateException.class);
    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("demo-boom");
    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void should_reuseGeneratedId_when_errorDispatchFollowsTheRequest() throws Exception {
    // Given a request without id that was handled once (id generated)
    filter.doFilter(request, response, (req, res) -> {});
    String generated = response.getHeader(CorrelationIdFilter.HEADER);
    assertThat(generated).satisfies(CorrelationIdFilterTest::isUuid);

    // When the container forwards the same request to the error page
    request.setDispatcherType(DispatcherType.ERROR);
    MockHttpServletResponse errorResponse = new MockHttpServletResponse();
    AtomicReference<String> seenByErrorHandler = new AtomicReference<>();
    filter.doFilter(
        request,
        errorResponse,
        (req, res) -> seenByErrorHandler.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

    // Then
    assertThat(errorResponse.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(generated);
    assertThat(seenByErrorHandler).hasValue(generated);
  }

  private static void isUuid(String value) {
    assertThat(value).isNotNull();
    assertThat(UUID.fromString(value).toString()).isEqualTo(value);
  }
}

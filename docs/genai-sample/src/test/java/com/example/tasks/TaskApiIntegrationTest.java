package com.example.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tasks.persistence.UserEntity;
import com.example.tasks.persistence.UserRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(
    properties = {
      "tasks.jwt.secret=" + TaskApiIntegrationTest.SECRET,
      "spring.jpa.properties.hibernate.generate_statistics=true"
    })
class TaskApiIntegrationTest {

  static final String SECRET = "integration-test-secret-of-at-least-32-bytes";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private TestRestTemplate rest;
  @Autowired private UserRepository users;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @BeforeEach
  void users() {
    if (users.findByUsername("alice").isEmpty()) {
      users.save(new UserEntity(UUID.randomUUID(), "alice"));
      users.save(new UserEntity(UUID.randomUUID(), "bob"));
    }
  }

  @Test
  void fullLifecycle() throws Exception {
    HttpHeaders alice = bearer("alice");
    HttpHeaders bob = bearer("bob");

    ResponseEntity<Map> created =
        rest.exchange(
            "/api/v1/tasks",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("title", "Buy milk"), alice),
            Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String location = created.getHeaders().getLocation().toString();

    rest.exchange(
        "/api/v1/tasks", HttpMethod.POST, new HttpEntity<>(Map.of("title", "Bob's task"), bob), Map.class);

    ResponseEntity<Map> aliceTasks =
        rest.exchange("/api/v1/tasks", HttpMethod.GET, new HttpEntity<>(alice), Map.class);
    assertThat(aliceTasks.getBody().get("totalElements")).isEqualTo(1);

    ResponseEntity<Map> forBob =
        rest.exchange(location, HttpMethod.GET, new HttpEntity<>(bob), Map.class);
    assertThat(forBob.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> stale =
        rest.exchange(
            location,
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("title", "Buy oat milk", "status", "DONE", "version", 7), alice),
            Map.class);
    assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    ResponseEntity<Void> deleted =
        rest.exchange(location, HttpMethod.DELETE, new HttpEntity<>(alice), Void.class);
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<Map> gone =
        rest.exchange(location, HttpMethod.GET, new HttpEntity<>(alice), Map.class);
    assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ---- added during review (docs/GENAI.md, section 3) ---------------------------------------

  @Test
  void timestampsReadBackExactlyAsTheyWereReturned() throws Exception {
    HttpHeaders dave = bearer(user("dave"));

    ResponseEntity<Map> created =
        rest.exchange(
            "/api/v1/tasks", HttpMethod.POST, new HttpEntity<>(Map.of("title", "Precise"), dave), Map.class);
    ResponseEntity<Map> read =
        rest.exchange(
            created.getHeaders().getLocation().toString(),
            HttpMethod.GET,
            new HttpEntity<>(dave),
            Map.class);

    assertThat(read.getBody().get("createdAt")).isEqualTo(created.getBody().get("createdAt"));
    assertThat(read.getBody().get("updatedAt")).isEqualTo(created.getBody().get("updatedAt"));
  }

  @Test
  void creatingATaskInsertsWithoutReadingItFirst() throws Exception {
    HttpHeaders erin = bearer(user("erin"));
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    rest.exchange(
        "/api/v1/tasks", HttpMethod.POST, new HttpEntity<>(Map.of("title", "One insert"), erin), Map.class);

    // one query for the owner, one insert; no select-before-insert of the new task
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
  }

  @Test
  void listsOwnTasksByDueDateWithUndatedLastAndFilters() throws Exception {
    HttpHeaders carol = bearer(user("carol"));
    post(carol, Map.of("title", "December", "dueDate", "2099-12-01"));
    post(carol, Map.of("title", "Someday"));
    post(carol, Map.of("title", "November", "dueDate", "2099-11-01", "status", "DONE"));

    assertThat(titles(list(carol, ""))).containsExactly("November", "December", "Someday");
    assertThat(titles(list(carol, "?status=DONE"))).containsExactly("November");
    assertThat(titles(list(carol, "?dueBefore=2099-11-15"))).containsExactly("November");
  }

  private String user(String username) {
    if (users.findByUsername(username).isEmpty()) {
      users.save(new UserEntity(UUID.randomUUID(), username));
    }
    return username;
  }

  private void post(HttpHeaders headers, Map<String, Object> body) {
    ResponseEntity<Map> created =
        rest.exchange("/api/v1/tasks", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  private Map list(HttpHeaders headers, String query) {
    return rest.exchange("/api/v1/tasks" + query, HttpMethod.GET, new HttpEntity<>(headers), Map.class)
        .getBody();
  }

  @SuppressWarnings("unchecked")
  private static java.util.List<Object> titles(Map page) {
    return ((java.util.List<Map<String, Object>>) page.get("content"))
        .stream().map(task -> task.get("title")).toList();
  }

  private static HttpHeaders bearer(String username) throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(username)
            .issueTime(new Date())
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwt.serialize());
    return headers;
  }
}

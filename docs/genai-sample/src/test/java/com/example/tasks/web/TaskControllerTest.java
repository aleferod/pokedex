package com.example.tasks.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.tasks.persistence.TaskEntity;
import com.example.tasks.persistence.UserEntity;
import com.example.tasks.security.SecurityConfig;
import com.example.tasks.task.InvalidDueDateException;
import com.example.tasks.task.StaleTaskException;
import com.example.tasks.task.TaskNotFoundException;
import com.example.tasks.task.TaskService;
import com.example.tasks.task.TaskStatus;
import com.example.tasks.task.UnknownUserException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@TestPropertySource(properties = "tasks.jwt.secret=test-secret-that-is-at-least-32-bytes-long")
class TaskControllerTest {

  private static final UUID ID = UUID.fromString("7d1f9a52-1c1e-4c0a-9a43-1f2d3e4c5b6a");

  @Autowired private MockMvc mvc;
  @MockitoBean private TaskService service;

  @Test
  void createReturns201WithLocation() throws Exception {
    given(service.create(eq("alice"), any())).willReturn(task());

    mvc.perform(
            post("/api/v1/tasks")
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Write tests\",\"dueDate\":\"2026-03-12\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/tasks/" + ID))
        .andExpect(jsonPath("$.title").value("Write tests"));
  }

  @Test
  void blankTitleReturns400() throws Exception {
    mvc.perform(
            post("/api/v1/tasks")
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("title"));
  }

  @Test
  void unknownStatusReturns400() throws Exception {
    mvc.perform(
            post("/api/v1/tasks")
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"status\":\"WAITING\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("status"));
  }

  @Test
  void unknownFieldReturns400() throws Exception {
    mvc.perform(
            post("/api/v1/tasks")
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"priority\":\"HIGH\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("priority"));
  }

  @Test
  void pastDueDateReturns400() throws Exception {
    given(service.create(eq("alice"), any())).willThrow(new InvalidDueDateException());

    mvc.perform(
            post("/api/v1/tasks")
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"dueDate\":\"2020-01-01\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("dueDate"));
  }

  @Test
  void sizeAbove100Returns400() throws Exception {
    mvc.perform(get("/api/v1/tasks?size=101").with(jwt().jwt(token -> token.subject("alice"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void malformedIdReturns400() throws Exception {
    mvc.perform(get("/api/v1/tasks/not-a-uuid").with(jwt().jwt(token -> token.subject("alice"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("id"));
  }

  @Test
  void foreignTaskReturns404() throws Exception {
    given(service.get("alice", ID)).willThrow(new TaskNotFoundException(ID));

    mvc.perform(get("/api/v1/tasks/" + ID).with(jwt().jwt(token -> token.subject("alice"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void staleVersionReturns409() throws Exception {
    given(service.replace(eq("alice"), eq(ID), any())).willThrow(new StaleTaskException(ID));

    mvc.perform(
            put("/api/v1/tasks/" + ID)
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"status\":\"DONE\",\"version\":0}"))
        .andExpect(status().isConflict());
  }

  @Test
  void deleteReturns204() throws Exception {
    mvc.perform(delete("/api/v1/tasks/" + ID).with(jwt().jwt(token -> token.subject("alice"))))
        .andExpect(status().isNoContent());
  }

  @Test
  void missingTokenReturns401() throws Exception {
    mvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
  }

  @Test
  void unknownUserReturns401() throws Exception {
    given(service.get("ghost", ID)).willThrow(new UnknownUserException("ghost"));

    mvc.perform(get("/api/v1/tasks/" + ID).with(jwt().jwt(token -> token.subject("ghost"))))
        .andExpect(status().isUnauthorized());
  }

  // ---- added during review (docs/GENAI.md, section 3) ---------------------------------------

  @Test
  void missingTokenAnswersWithABearerChallengeAndAProblem() throws Exception {
    mvc.perform(get("/api/v1/tasks"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.instance").value("/api/v1/tasks"));
  }

  @Test
  void invalidTokenReturns401WithInvalidTokenChallenge() throws Exception {
    mvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(
            header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("invalid_token")));
  }

  @Test
  void unknownUserAnswersWithABearerChallenge() throws Exception {
    given(service.get("ghost", ID)).willThrow(new UnknownUserException("ghost"));

    mvc.perform(get("/api/v1/tasks/" + ID).with(jwt().jwt(token -> token.subject("ghost"))))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")));
  }

  @Test
  void unexpectedErrorReturns500ProblemWithoutInternals() throws Exception {
    given(service.get("alice", ID))
        .willThrow(new IllegalStateException("connection to db-prod:5432 refused"));

    mvc.perform(get("/api/v1/tasks/" + ID).with(jwt().jwt(token -> token.subject("alice"))))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("db-prod"))))
        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("IllegalStateException"))));
  }

  @Test
  void titleOf120CharactersAfterTrimmingIsAccepted() throws Exception {
    given(service.create(eq("alice"), any())).willReturn(task());
    String title = "  " + "a".repeat(120) + "  ";

    mvc.perform(
            post("/api/v1/tasks")
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void titleOf121CharactersAfterTrimmingIsRejected() throws Exception {
    mvc.perform(
            post("/api/v1/tasks")
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + "a".repeat(121) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("title"));
  }

  @Test
  void tooLongDescriptionReturns400() throws Exception {
    mvc.perform(
            post("/api/v1/tasks")
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"description\":\"" + "d".repeat(2001) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("description"));
  }

  @Test
  void malformedDueDateReturns400NamingTheField() throws Exception {
    mvc.perform(
            post("/api/v1/tasks")
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"dueDate\":\"2026-02-30\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("dueDate"));
  }

  @Test
  void malformedJsonReturns400Problem() throws Exception {
    mvc.perform(
            post("/api/v1/tasks")
                .with(jwt().jwt(token -> token.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void negativePageReturns400NamingIt() throws Exception {
    mvc.perform(get("/api/v1/tasks?page=-1").with(jwt().jwt(token -> token.subject("alice"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("page"));
  }

  @Test
  void unknownStatusFilterReturns400NamingIt() throws Exception {
    mvc.perform(
            get("/api/v1/tasks?status=WAITING").with(jwt().jwt(token -> token.subject("alice"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("status"));
  }

  private static TaskEntity task() {
    return new TaskEntity(
        ID,
        new UserEntity(UUID.randomUUID(), "alice"),
        "Write tests",
        null,
        TaskStatus.TODO,
        LocalDate.parse("2026-03-12"),
        Instant.parse("2026-03-10T10:00:00Z"));
  }
}

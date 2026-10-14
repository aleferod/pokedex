package com.ballastlane.pokedex.adapter.in.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.user.DuplicateUserException;
import com.ballastlane.pokedex.domain.user.InvalidCredentialsException;
import com.ballastlane.pokedex.domain.user.UserId;
import com.ballastlane.pokedex.domain.user.UserNotFoundException;
import com.ballastlane.pokedex.domain.user.password.WeakPasswordException;
import com.ballastlane.pokedex.support.WebSliceTest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 400 field-level errors for bad input, and how domain exceptions become HTTP problems. */
@WebSliceTest
@Import(InputAndDomainErrorsTest.ProbeController.class)
class InputAndDomainErrorsTest {

  @Autowired private MockMvcTester mvc;

  @Test
  void should_listTheField_when_requiredFieldIsMissing() {
    MvcTestResult result = post("{\"email\":\"ash@pallet.town\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(errorFields(result)).containsExactly("username");
  }

  @Test
  void should_listEveryInvalidField_when_severalFieldsAreInvalid() {
    MvcTestResult result = post("{\"username\":\"a\",\"email\":\"not-an-email\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(errorFields(result)).containsExactlyInAnyOrder("username", "email");
    assertThat(result).bodyJson().extractingPath("$.errors[0].message").asString().isNotBlank();
  }

  @Test
  void should_nameTheMember_when_bodyHasAnUnknownMember() {
    MvcTestResult result =
        post("{\"username\":\"ash\",\"email\":\"ash@pallet.town\",\"role\":\"ADMIN\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(errorFields(result)).containsExactly("role");
  }

  @Test
  void should_nameTheField_when_valueHasTheWrongJsonType() {
    MvcTestResult result = post("{\"username\":{\"nested\":true},\"email\":\"ash@pallet.town\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(errorFields(result)).containsExactly("username");
  }

  @Test
  void should_nameTheField_when_numberFieldGetsText() {
    MvcTestResult result =
        post("{\"username\":\"ash\",\"email\":\"ash@pallet.town\",\"count\":\"many\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(errorFields(result)).containsExactly("count");
  }

  @Test
  void should_sayMalformedWithoutFields_when_bodyIsNotJson() {
    MvcTestResult result = post("{\"username\": ");

    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.detail")
        .asString()
        .containsIgnoringCase("malformed");
    assertThat(result).bodyJson().extractingPath("$.errors").asArray().isEmpty();
  }

  @Test
  void should_return400WithTheField_when_domainRejectsAValue() {
    MvcTestResult result = mvc.get().uri("/test/invalid-field").exchange();

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("username");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[0].message")
        .isEqualTo("must be 3 to 30 characters");
  }

  @Test
  void should_listEveryViolation_when_passwordIsWeak() {
    MvcTestResult result = mvc.get().uri("/test/weak-password").exchange();

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(errorFields(result)).containsExactly("password", "password");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].message")
        .asArray()
        .containsExactly("must contain a digit", "must contain an upper-case letter");
  }

  @Test
  void should_return409NamingTheField_when_accountAlreadyExists() {
    MvcTestResult result = mvc.get().uri("/test/duplicate").exchange();

    assertThat(result).hasStatus(HttpStatus.CONFLICT);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#conflict");
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("email");
  }

  @Test
  void should_return401WithFixedDetail_when_credentialsAreInvalid() {
    MvcTestResult result = mvc.get().uri("/test/bad-credentials").exchange();

    assertThat(result)
        .hasStatus(HttpStatus.UNAUTHORIZED)
        .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#unauthorized");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Invalid username or password.");
  }

  @Test
  void should_return401_when_tokenAccountNoLongerExists() {
    MvcTestResult result = mvc.get().uri("/test/user-gone").exchange();

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#unauthorized");
  }

  private MvcTestResult post(String json) {
    return mvc.post()
        .uri("/test/body")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  @SuppressWarnings("unchecked")
  private static List<String> errorFields(MvcTestResult result) {
    try {
      Map<String, Object> body =
          new tools.jackson.databind.json.JsonMapper()
              .readValue(result.getResponse().getContentAsString(), Map.class);
      return ((List<Map<String, Object>>) body.get("errors"))
          .stream().map(error -> (String) error.get("field")).toList();
    } catch (java.io.UnsupportedEncodingException unreadable) {
      throw new IllegalStateException(unreadable);
    }
  }

  record ProbeBody(
      @NotBlank @Size(min = 3, max = 30) String username,
      @NotBlank @Email String email,
      Integer count) {}

  @RestController
  static class ProbeController {

    @PostMapping("/test/body")
    String body(@Valid @RequestBody ProbeBody body) {
      return "ok " + body.username();
    }

    @GetMapping("/test/invalid-field")
    String invalidField() {
      throw new InvalidFieldException("username", "must be 3 to 30 characters");
    }

    @GetMapping("/test/weak-password")
    String weakPassword() {
      throw new WeakPasswordException(
          List.of("must contain a digit", "must contain an upper-case letter"));
    }

    @GetMapping("/test/duplicate")
    String duplicate() {
      throw new DuplicateUserException("email");
    }

    @GetMapping("/test/bad-credentials")
    String badCredentials() {
      throw new InvalidCredentialsException();
    }

    @GetMapping("/test/user-gone")
    String userGone() {
      throw new UserNotFoundException(UserId.random());
    }
  }
}

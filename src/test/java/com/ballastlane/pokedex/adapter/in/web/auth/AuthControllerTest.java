package com.ballastlane.pokedex.adapter.in.web.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ballastlane.pokedex.application.port.in.user.AuthenticateUser;
import com.ballastlane.pokedex.application.port.in.user.AuthenticatedUser;
import com.ballastlane.pokedex.application.port.in.user.Credentials;
import com.ballastlane.pokedex.application.port.in.user.GetCurrentUser;
import com.ballastlane.pokedex.application.port.in.user.RegisterUser;
import com.ballastlane.pokedex.application.port.in.user.RegisterUserCommand;
import com.ballastlane.pokedex.application.port.out.user.IssuedToken;
import com.ballastlane.pokedex.application.port.out.user.TokenPort;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.user.DuplicateUserException;
import com.ballastlane.pokedex.domain.user.InvalidCredentialsException;
import com.ballastlane.pokedex.domain.user.Role;
import com.ballastlane.pokedex.domain.user.User;
import com.ballastlane.pokedex.domain.user.password.WeakPasswordException;
import com.ballastlane.pokedex.support.TestTokens;
import com.ballastlane.pokedex.support.WebSliceTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebSliceTest(controllers = AuthController.class)
class AuthControllerTest {

  private static final String REGISTER = "/api/v1/auth/register";
  private static final String LOGIN = "/api/v1/auth/login";
  private static final String ME = "/api/v1/auth/me";
  private static final User ASH = TestTokens.user("ash", Role.USER);

  @Autowired private MockMvcTester mvc;
  @Autowired private TokenPort tokenPort;
  @Autowired private JwtEncoder jwtEncoder;

  @MockitoBean private RegisterUser registerUser;
  @MockitoBean private AuthenticateUser authenticateUser;
  @MockitoBean private GetCurrentUser getCurrentUser;

  private TestTokens tokens;

  @BeforeEach
  void setUp() {
    tokens = new TestTokens(tokenPort, jwtEncoder);
  }

  // ---- register --------------------------------------------------------------------------------

  @Test
  void should_return201WithAccountButNoPassword_when_registrationSucceeds() {
    // Given
    given(registerUser.register(any())).willReturn(ASH);

    // When
    MvcTestResult result =
        postJson(
            REGISTER,
            "{\"username\":\"ash\",\"email\":\"ash@pallet.town\",\"password\":\"Pikachu@25\"}");

    // Then
    assertThat(result).hasStatus(HttpStatus.CREATED).doesNotContainHeader(HttpHeaders.SET_COOKIE);
    assertThat(result)
        .bodyJson()
        .extractingPath("$")
        .asMap()
        .containsOnlyKeys("id", "username", "email", "role", "createdAt");
    assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(ASH.id().value().toString());
    assertThat(result).bodyJson().extractingPath("$.role").isEqualTo("USER");
    assertThat(result).bodyJson().extractingPath("$.createdAt").isEqualTo("2026-10-14T12:00:00Z");
    then(registerUser)
        .should()
        .register(new RegisterUserCommand("ash", "ash@pallet.town", "Pikachu@25"));
  }

  @Test
  void should_rejectWithoutRegistering_when_callerTriesToChooseARole() {
    MvcTestResult result =
        postJson(
            REGISTER,
            "{\"username\":\"ash\",\"email\":\"ash@pallet.town\",\"password\":\"Pikachu@25\","
                + "\"role\":\"ADMIN\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("role");
    then(registerUser).shouldHaveNoInteractions();
  }

  @Test
  void should_listEveryMissingField_when_bodyIsEmpty() {
    MvcTestResult result = postJson(REGISTER, "{}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].field")
        .asArray()
        .containsExactly("email", "password", "username");
    then(registerUser).shouldHaveNoInteractions();
  }

  @Test
  void should_return400NamingTheField_when_domainRejectsTheUsername() {
    given(registerUser.register(any()))
        .willThrow(new InvalidFieldException("username", "must be 3 to 30 characters"));

    MvcTestResult result =
        postJson(
            REGISTER,
            "{\"username\":\"a!\",\"email\":\"ash@pallet.town\",\"password\":\"Pikachu@25\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("username");
  }

  @Test
  void should_return400ListingViolations_when_passwordIsWeak() {
    given(registerUser.register(any()))
        .willThrow(
            new WeakPasswordException(
                List.of(
                    "must contain an upper-case letter",
                    "must contain a digit",
                    "must contain a character that is neither a letter nor a digit")));

    MvcTestResult result =
        postJson(
            REGISTER,
            "{\"username\":\"ash\",\"email\":\"ash@pallet.town\",\"password\":\"password\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors").asArray().hasSize(3);
  }

  @Test
  void should_return409NamingTheField_when_usernameIsTaken() {
    given(registerUser.register(any())).willThrow(new DuplicateUserException("username"));

    MvcTestResult result =
        postJson(
            REGISTER,
            "{\"username\":\"ASH\",\"email\":\"other@pallet.town\",\"password\":\"Pikachu@25\"}");

    assertThat(result).hasStatus(HttpStatus.CONFLICT);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("username");
  }

  // ---- login -----------------------------------------------------------------------------------

  @Test
  void should_returnBearerTokenAndAccount_when_credentialsAreValid() {
    // Given
    Instant expiresAt = Instant.parse("2026-10-14T13:00:00Z");
    given(authenticateUser.authenticate(new Credentials("Ash", "Pikachu@25")))
        .willReturn(new AuthenticatedUser(ASH, new IssuedToken("signed.jwt.token", expiresAt)));

    // When
    MvcTestResult result = postJson(LOGIN, "{\"username\":\"Ash\",\"password\":\"Pikachu@25\"}");

    // Then
    assertThat(result).hasStatus(HttpStatus.OK).doesNotContainHeader(HttpHeaders.SET_COOKIE);
    assertThat(result).bodyJson().extractingPath("$.accessToken").isEqualTo("signed.jwt.token");
    assertThat(result).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
    assertThat(result).bodyJson().extractingPath("$.expiresAt").isEqualTo("2026-10-14T13:00:00Z");
    assertThat(result).bodyJson().extractingPath("$.user.username").isEqualTo("ash");
  }

  @Test
  void should_return401WithFixedDetail_when_credentialsAreInvalid() {
    given(authenticateUser.authenticate(any())).willThrow(new InvalidCredentialsException());

    MvcTestResult result = postJson(LOGIN, "{\"username\":\"nobody\",\"password\":\"Wrong@1234\"}");

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Invalid username or password.");
  }

  @Test
  void should_return400NamingThePassword_when_passwordIsMissing() {
    MvcTestResult result = postJson(LOGIN, "{\"username\":\"ash\"}");

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("password");
    then(authenticateUser).shouldHaveNoInteractions();
  }

  // ---- me --------------------------------------------------------------------------------------

  @Test
  void should_returnTheCaller_when_tokenIsValid() {
    // Given
    given(getCurrentUser.byId(ASH.id())).willReturn(ASH);

    // When
    MvcTestResult result =
        mvc.get().uri(ME).header(HttpHeaders.AUTHORIZATION, tokens.bearer(ASH)).exchange();

    // Then
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result).bodyJson().extractingPath("$.username").isEqualTo("ash");
    assertThat(result).bodyJson().extractingPath("$.role").isEqualTo("USER");
  }

  @Test
  void should_return401WithBearerChallenge_when_noTokenIsSent() {
    MvcTestResult result = mvc.get().uri(ME).exchange();

    assertThat(result)
        .hasStatus(HttpStatus.UNAUTHORIZED)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#unauthorized");
  }

  private MvcTestResult postJson(String uri, String json) {
    return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
  }
}

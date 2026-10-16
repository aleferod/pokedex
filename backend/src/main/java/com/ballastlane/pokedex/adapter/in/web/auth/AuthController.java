package com.ballastlane.pokedex.adapter.in.web.auth;

import com.ballastlane.pokedex.application.port.in.user.AuthenticateUser;
import com.ballastlane.pokedex.application.port.in.user.GetCurrentUser;
import com.ballastlane.pokedex.application.port.in.user.RegisterUser;
import com.ballastlane.pokedex.domain.user.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Auxiliary auth API: register, log in, and see who the current token belongs to. */
@RestController
@RequestMapping(AuthController.BASE_PATH)
@Tag(name = "Authentication", description = "Accounts and bearer tokens")
public class AuthController {

  public static final String BASE_PATH = "/api/v1/auth";

  private final RegisterUser registerUser;
  private final AuthenticateUser authenticateUser;
  private final GetCurrentUser getCurrentUser;

  public AuthController(
      RegisterUser registerUser, AuthenticateUser authenticateUser, GetCurrentUser getCurrentUser) {
    this.registerUser = registerUser;
    this.authenticateUser = authenticateUser;
    this.getCurrentUser = getCurrentUser;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a regular (USER) account")
  @ApiResponse(responseCode = "201", description = "Account created")
  @ApiResponse(responseCode = "400", description = "Invalid fields or weak password")
  @ApiResponse(responseCode = "409", description = "Username or email already taken")
  public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    return AuthWebMapper.toResponse(registerUser.register(AuthWebMapper.toCommand(request)));
  }

  @PostMapping("/login")
  @Operation(summary = "Exchange credentials for a bearer access token")
  @ApiResponse(responseCode = "200", description = "Authenticated")
  @ApiResponse(responseCode = "401", description = "Invalid username or password")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return AuthWebMapper.toResponse(
        authenticateUser.authenticate(AuthWebMapper.toCredentials(request)));
  }

  @GetMapping("/me")
  @Operation(summary = "The account the bearer token belongs to")
  @SecurityRequirement(name = "bearer-jwt")
  @ApiResponse(responseCode = "200", description = "Current account")
  @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token")
  public UserResponse me(@AuthenticationPrincipal Jwt token) {
    return AuthWebMapper.toResponse(getCurrentUser.byId(UserId.of(token.getSubject())));
  }
}

package com.ballastlane.pokedex.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserIdTest {

  @Test
  void should_generateDistinctIds_when_randomIsCalled() {
    assertThat(UserId.random()).isNotEqualTo(UserId.random());
    assertThat(UserId.random().value()).isNotNull();
  }

  @Test
  void should_parseUuid_when_textIsValid() {
    UUID uuid = UUID.randomUUID();
    assertThat(UserId.of(uuid.toString())).isEqualTo(new UserId(uuid));
  }

  @Test
  void should_rejectNamingTheField_when_textIsNotAUuid() {
    assertThatThrownBy(() -> UserId.of("not-a-uuid"))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("id"));
  }

  @Test
  void should_reject_when_valueIsNull() {
    assertThatThrownBy(() -> new UserId(null)).isInstanceOf(InvalidFieldException.class);
  }
}

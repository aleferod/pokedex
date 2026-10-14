package com.ballastlane.pokedex.application.usecase.localpokemon;

import static com.ballastlane.pokedex.application.usecase.localpokemon.LocalPokemonTestData.IMPORTED;
import static com.ballastlane.pokedex.application.usecase.localpokemon.LocalPokemonTestData.NOW;
import static com.ballastlane.pokedex.application.usecase.localpokemon.LocalPokemonTestData.storedPikachu;
import static com.ballastlane.pokedex.application.usecase.localpokemon.LocalPokemonTestData.withVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ballastlane.pokedex.application.port.in.localpokemon.ExpectedVersion;
import com.ballastlane.pokedex.application.port.in.localpokemon.ProprietaryValues;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.AuditInfo;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException;
import com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldsException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplaceLocalPokemonUseCaseTest {

  private static final ProprietaryValues JOHTO_ONLY =
      new ProprietaryValues(null, "Johto", null, null, null);

  @Mock private LocalPokemonRepositoryPort repository;

  private ReplaceLocalPokemonUseCase replaceLocalPokemon;

  @BeforeEach
  void setUp() {
    replaceLocalPokemon =
        new ReplaceLocalPokemonUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void should_replaceEveryProprietaryField_when_versionMatches() {
    // Given
    LocalPokemon stored = storedPikachu(3);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));
    given(repository.save(any()))
        .willAnswer(invocation -> withVersion(invocation.getArgument(0), 4));

    // When
    LocalPokemon replaced =
        replaceLocalPokemon.replace(stored.id(), ExpectedVersion.exactly(3), JOHTO_ONLY);

    // Then
    LocalPokemon saved = saved();
    assertThat(saved.proprietary().region()).isEqualTo("Johto");
    assertThat(saved.proprietary().notes()).isNull();
    assertThat(saved.proprietary().localizedNames().names()).isEmpty();
    assertThat(saved.proprietary().internalTags().tags()).isEmpty();
    assertThat(saved.upstream()).isEqualTo(stored.upstream());
    assertThat(saved.audit()).isEqualTo(new AuditInfo(IMPORTED, NOW, IMPORTED, "ash"));
    assertThat(saved.version()).isEqualTo(3L);
    assertThat(replaced.version()).isEqualTo(4L);
  }

  @Test
  void should_replace_when_anyVersionIsAccepted() {
    LocalPokemon stored = storedPikachu(7);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));
    given(repository.save(any()))
        .willAnswer(invocation -> withVersion(invocation.getArgument(0), 8));

    assertThat(
            replaceLocalPokemon.replace(stored.id(), ExpectedVersion.any(), JOHTO_ONLY).version())
        .isEqualTo(8L);
  }

  @Test
  void should_throwNotFound_when_idIsUnknown() {
    LocalPokemonId unknown = LocalPokemonId.random();
    given(repository.findById(unknown)).willReturn(Optional.empty());

    assertThatThrownBy(
            () -> replaceLocalPokemon.replace(unknown, ExpectedVersion.exactly(0), JOHTO_ONLY))
        .isInstanceOf(LocalPokemonNotFoundException.class);
  }

  @Test
  void should_changeNothing_when_expectedVersionIsStale() {
    LocalPokemon stored = storedPikachu(3);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));

    assertThatThrownBy(
            () -> replaceLocalPokemon.replace(stored.id(), ExpectedVersion.exactly(2), JOHTO_ONLY))
        .isInstanceOf(StaleLocalPokemonException.class);
    then(repository).should(never()).save(any());
  }

  @Test
  void should_reportEveryInvalidField_when_valuesBreakTheRules() {
    LocalPokemon stored = storedPikachu(3);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));
    ProprietaryValues invalid =
        new ProprietaryValues(null, "x".repeat(51), null, List.of("Bad Tag!"), null);

    assertThatThrownBy(
            () -> replaceLocalPokemon.replace(stored.id(), ExpectedVersion.exactly(3), invalid))
        .isInstanceOfSatisfying(
            InvalidFieldsException.class,
            error ->
                assertThat(error.violations())
                    .extracting(InvalidFieldException::field)
                    .containsExactlyInAnyOrder("region", "internalTags"));
    then(repository).should(never()).save(any());
  }

  @Test
  void should_reportStale_when_aConcurrentWriterWinsAtSave() {
    LocalPokemon stored = storedPikachu(3);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));
    given(repository.save(any())).willThrow(new StaleLocalPokemonException(stored.id()));

    assertThatThrownBy(
            () -> replaceLocalPokemon.replace(stored.id(), ExpectedVersion.exactly(3), JOHTO_ONLY))
        .isInstanceOf(StaleLocalPokemonException.class);
  }

  private LocalPokemon saved() {
    ArgumentCaptor<LocalPokemon> saved = ArgumentCaptor.forClass(LocalPokemon.class);
    then(repository).should().save(saved.capture());
    return saved.getValue();
  }
}

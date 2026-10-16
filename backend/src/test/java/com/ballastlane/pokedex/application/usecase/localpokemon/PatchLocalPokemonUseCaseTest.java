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
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.AuditInfo;
import com.ballastlane.pokedex.domain.localpokemon.FieldPatch;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNamesPatch;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryPatch;
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
class PatchLocalPokemonUseCaseTest {

  private static final ProprietaryPatch NEW_NOTES =
      new ProprietaryPatch(null, null, null, null, FieldPatch.set("Loves ketchup"));

  @Mock private LocalPokemonRepositoryPort repository;

  private PatchLocalPokemonUseCase patchLocalPokemon;

  @BeforeEach
  void setUp() {
    patchLocalPokemon = new PatchLocalPokemonUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void should_changeOnlyThePatchedField_when_versionMatches() {
    // Given
    LocalPokemon stored = storedPikachu(3);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));
    given(repository.save(any()))
        .willAnswer(invocation -> withVersion(invocation.getArgument(0), 4));

    // When
    LocalPokemon patched =
        patchLocalPokemon.patch(stored.id(), ExpectedVersion.exactly(3), NEW_NOTES);

    // Then
    ArgumentCaptor<LocalPokemon> saved = ArgumentCaptor.forClass(LocalPokemon.class);
    then(repository).should().save(saved.capture());
    assertThat(saved.getValue().proprietary().notes()).isEqualTo("Loves ketchup");
    assertThat(saved.getValue().proprietary().region()).isEqualTo("Kanto");
    assertThat(saved.getValue().proprietary().localizedNames())
        .isEqualTo(stored.proprietary().localizedNames());
    assertThat(saved.getValue().audit()).isEqualTo(new AuditInfo(IMPORTED, NOW, IMPORTED, "ash"));
    assertThat(patched.version()).isEqualTo(4L);
  }

  @Test
  void should_throwNotFound_when_idIsUnknown() {
    LocalPokemonId unknown = LocalPokemonId.random();
    given(repository.findById(unknown)).willReturn(Optional.empty());

    assertThatThrownBy(() -> patchLocalPokemon.patch(unknown, ExpectedVersion.any(), NEW_NOTES))
        .isInstanceOf(LocalPokemonNotFoundException.class);
  }

  @Test
  void should_changeNothing_when_expectedVersionIsStale() {
    LocalPokemon stored = storedPikachu(4);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));

    assertThatThrownBy(
            () -> patchLocalPokemon.patch(stored.id(), ExpectedVersion.exactly(3), NEW_NOTES))
        .isInstanceOf(StaleLocalPokemonException.class);
    then(repository).should(never()).save(any());
  }

  @Test
  void should_reportEveryInvalidField_when_thePatchBreaksTheRules() {
    LocalPokemon stored = storedPikachu(3);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));
    ProprietaryPatch invalid =
        new ProprietaryPatch(
            LocalizedNamesPatch.keep(),
            FieldPatch.set("x".repeat(51)),
            null,
            FieldPatch.set(List.of("Bad Tag!")),
            null);

    assertThatThrownBy(
            () -> patchLocalPokemon.patch(stored.id(), ExpectedVersion.exactly(3), invalid))
        .isInstanceOfSatisfying(
            InvalidFieldsException.class,
            error ->
                assertThat(error.violations())
                    .extracting(InvalidFieldException::field)
                    .containsExactlyInAnyOrder("region", "internalTags"));
    then(repository).should(never()).save(any());
  }
}

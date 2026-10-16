package com.ballastlane.pokedex.application.usecase.localpokemon;

import static com.ballastlane.pokedex.application.usecase.localpokemon.LocalPokemonTestData.storedPikachu;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ballastlane.pokedex.application.port.in.localpokemon.ExpectedVersion;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException;
import com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteLocalPokemonUseCaseTest {

  @Mock private LocalPokemonRepositoryPort repository;
  @InjectMocks private DeleteLocalPokemonUseCase deleteLocalPokemon;

  @Test
  void should_deleteTheStoredRecord_when_noVersionIsExpected() {
    LocalPokemon stored = storedPikachu(5);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));

    deleteLocalPokemon.delete(stored.id(), ExpectedVersion.any());

    then(repository).should().delete(stored);
  }

  @Test
  void should_deleteTheStoredRecord_when_versionMatches() {
    LocalPokemon stored = storedPikachu(5);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));

    deleteLocalPokemon.delete(stored.id(), ExpectedVersion.exactly(5));

    then(repository).should().delete(stored);
  }

  @Test
  void should_keepTheRecord_when_expectedVersionIsStale() {
    LocalPokemon stored = storedPikachu(5);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));

    assertThatThrownBy(() -> deleteLocalPokemon.delete(stored.id(), ExpectedVersion.exactly(4)))
        .isInstanceOf(StaleLocalPokemonException.class);
    then(repository).should(never()).delete(any());
  }

  @Test
  void should_throwNotFound_when_idIsUnknown() {
    LocalPokemonId unknown = LocalPokemonId.random();
    given(repository.findById(unknown)).willReturn(Optional.empty());

    assertThatThrownBy(() -> deleteLocalPokemon.delete(unknown, ExpectedVersion.any()))
        .isInstanceOf(LocalPokemonNotFoundException.class);
    then(repository).should(never()).delete(any());
  }
}

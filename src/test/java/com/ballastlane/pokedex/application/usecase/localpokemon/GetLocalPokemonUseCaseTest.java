package com.ballastlane.pokedex.application.usecase.localpokemon;

import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonNotFoundException;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetLocalPokemonUseCaseTest {

  @Mock private LocalPokemonRepositoryPort repository;
  @InjectMocks private GetLocalPokemonUseCase getLocalPokemon;

  @Test
  void should_returnTheRecord_when_itExists() {
    LocalPokemon stored =
        LocalPokemon.importFrom(
            LocalPokemonId.random(), new PokemonId(25), pikachu(60), "ash", Instant.EPOCH);
    given(repository.findById(stored.id())).willReturn(Optional.of(stored));

    assertThat(getLocalPokemon.get(stored.id())).isEqualTo(stored);
  }

  @Test
  void should_throwNotFound_when_idIsUnknown() {
    LocalPokemonId unknown = LocalPokemonId.random();
    given(repository.findById(unknown)).willReturn(Optional.empty());

    assertThatThrownBy(() -> getLocalPokemon.get(unknown))
        .isInstanceOf(LocalPokemonNotFoundException.class)
        .hasMessageContaining(unknown.value().toString());
  }
}

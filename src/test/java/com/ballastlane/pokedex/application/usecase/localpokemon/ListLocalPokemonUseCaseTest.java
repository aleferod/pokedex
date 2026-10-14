package com.ballastlane.pokedex.application.usecase.localpokemon;

import static com.ballastlane.pokedex.application.usecase.localpokemon.LocalPokemonTestData.storedPikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFilter;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListLocalPokemonUseCaseTest {

  @Mock private LocalPokemonRepositoryPort repository;
  @InjectMocks private ListLocalPokemonUseCase listLocalPokemon;

  @Test
  void should_returnTheMatchingPage_when_listing() {
    LocalPokemonFilter filter = new LocalPokemonFilter("starter", null);
    PageRequest request = new PageRequest(0, 20);
    Page<LocalPokemon> page = Page.of(List.of(storedPikachu(0)), request, 1);
    given(repository.findAll(filter, request)).willReturn(page);

    assertThat(listLocalPokemon.list(filter, request)).isEqualTo(page);
  }
}

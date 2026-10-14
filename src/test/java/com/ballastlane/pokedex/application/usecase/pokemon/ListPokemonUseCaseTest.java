package com.ballastlane.pokedex.application.usecase.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.pokemon.Weight;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListPokemonUseCaseTest {

  @Mock private PokemonCatalogPort catalog;
  @InjectMocks private ListPokemonUseCase listPokemon;

  @Test
  void should_returnTheCatalogPage_when_requested() {
    // Given
    PageRequest request = new PageRequest(0, 1);
    Page<PokemonSummary> page =
        Page.of(
            List.of(
                new PokemonSummary(
                    new PokemonId(1),
                    "bulbasaur",
                    "https://img/1.png",
                    "Seed Pokémon",
                    Weight.ofHectograms(69),
                    List.of("overgrow", "chlorophyll"))),
            request,
            1351);
    given(catalog.findPage(request)).willReturn(page);

    // When / Then
    assertThat(listPokemon.list(request)).isEqualTo(page);
  }

  @Test
  void should_propagateUnavailability_when_catalogCannotAnswer() {
    // Given
    PageRequest request = new PageRequest(0, 20);
    given(catalog.findPage(request))
        .willThrow(new CatalogUnavailableException("PokeAPI unreachable", null));

    // When / Then
    assertThatThrownBy(() -> listPokemon.list(request))
        .isInstanceOf(CatalogUnavailableException.class);
  }
}

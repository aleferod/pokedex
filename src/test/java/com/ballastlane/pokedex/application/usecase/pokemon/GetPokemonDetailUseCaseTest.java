package com.ballastlane.pokedex.application.usecase.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.domain.pokemon.BaseStats;
import com.ballastlane.pokedex.domain.pokemon.EvolutionStage;
import com.ballastlane.pokedex.domain.pokemon.Height;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonNotFoundException;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.pokemon.Weight;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPokemonDetailUseCaseTest {

  @Mock private PokemonCatalogPort catalog;
  @InjectMocks private GetPokemonDetailUseCase getPokemonDetail;

  @Test
  void should_returnTheDetail_when_thePokemonExists() {
    // Given
    PokemonIdentifier ditto = PokemonIdentifier.parse("ditto");
    PokemonDetail detail =
        new PokemonDetail(
            new PokemonId(132),
            "ditto",
            "https://img/sprite-132.png",
            "https://img/132.png",
            "Transform Pokémon",
            "It can transform into anything.",
            List.of("normal"),
            Height.ofDecimetres(3),
            Weight.ofHectograms(40),
            List.of("limber", "imposter"),
            new BaseStats(48, 48, 48, 48, 48, 48),
            new EvolutionStage(new PokemonId(132), "ditto", null, List.of()));
    given(catalog.findDetail(ditto)).willReturn(Optional.of(detail));

    // When / Then
    assertThat(getPokemonDetail.get(ditto)).isEqualTo(detail);
  }

  @Test
  void should_throwNotFoundNamingTheIdentifier_when_noPokemonMatches() {
    // Given
    PokemonIdentifier missingno = PokemonIdentifier.parse("missingno");
    given(catalog.findDetail(missingno)).willReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> getPokemonDetail.get(missingno))
        .isInstanceOfSatisfying(
            PokemonNotFoundException.class,
            error -> {
              assertThat(error.identifier()).isEqualTo("missingno");
              assertThat(error).hasMessageContaining("missingno");
            });
  }

  @Test
  void should_readTheSourceDirectly_when_aFreshDetailIsRequested() {
    PokemonIdentifier ditto = PokemonIdentifier.parse("ditto");
    PokemonDetail detail = ditto();
    given(catalog.findFreshDetail(ditto)).willReturn(Optional.of(detail));

    assertThat(getPokemonDetail.getFresh(ditto)).isEqualTo(detail);
    then(catalog).should(never()).findDetail(any());
  }

  @Test
  void should_throwNotFound_when_aFreshLookupFindsNothing() {
    PokemonIdentifier missingno = PokemonIdentifier.parse("missingno");
    given(catalog.findFreshDetail(missingno)).willReturn(Optional.empty());

    assertThatThrownBy(() -> getPokemonDetail.getFresh(missingno))
        .isInstanceOf(PokemonNotFoundException.class)
        .hasMessageContaining("missingno");
  }

  @Test
  void should_fallBackToTheOrdinaryLookup_when_aPortHasNoCacheToBypass() {
    PokemonDetail detail = ditto();
    PokemonCatalogPort plain =
        new PokemonCatalogPort() {
          @Override
          public Page<PokemonSummary> findPage(PageRequest request) {
            throw new UnsupportedOperationException();
          }

          @Override
          public Optional<PokemonDetail> findDetail(PokemonIdentifier identifier) {
            return Optional.of(detail);
          }
        };

    assertThat(plain.findFreshDetail(PokemonIdentifier.parse("ditto"))).contains(detail);
  }

  private static PokemonDetail ditto() {
    return new PokemonDetail(
        new PokemonId(132),
        "ditto",
        "https://img/sprite-132.png",
        "https://img/132.png",
        "Transform Pokémon",
        "It can transform into anything.",
        List.of("normal"),
        Height.ofDecimetres(3),
        Weight.ofHectograms(40),
        List.of("limber", "imposter"),
        new BaseStats(48, 48, 48, 48, 48, 48),
        new EvolutionStage(new PokemonId(132), "ditto", null, List.of()));
  }

  @Test
  void should_propagateUnavailability_when_catalogCannotAnswer() {
    PokemonIdentifier one = PokemonIdentifier.parse("1");
    given(catalog.findDetail(one))
        .willThrow(new CatalogUnavailableException("PokeAPI unreachable", null));

    assertThatThrownBy(() -> getPokemonDetail.get(one))
        .isInstanceOf(CatalogUnavailableException.class);
  }
}

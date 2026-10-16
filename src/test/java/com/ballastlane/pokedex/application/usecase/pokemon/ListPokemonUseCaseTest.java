package com.ballastlane.pokedex.application.usecase.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.application.port.out.pokemon.PokemonCatalogPort;
import com.ballastlane.pokedex.domain.pokemon.CatalogName;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonNameQuery;
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

  /** The name index, in catalog order (a subset of PokeAPI's real list). */
  private static final List<CatalogName> NAMES =
      List.of(
          name(1, "bulbasaur"),
          name(2, "ivysaur"),
          name(3, "venusaur"),
          name(25, "pikachu"),
          name(26, "raichu"),
          name(172, "pichu"),
          name(10080, "pikachu-rock-star"),
          name(10081, "pikachu-belle"));

  @Test
  void should_returnOnlyTheMatchingEntriesInCatalogOrder_when_searchingByName() {
    // Given
    given(catalog.findAllNames()).willReturn(NAMES);
    List<PokemonId> matches = ids(25, 10080, 10081);
    given(catalog.findSummaries(matches))
        .willReturn(
            List.of(
                summary(25, "pikachu"),
                summary(10080, "pikachu-rock-star"),
                summary(10081, "pikachu-belle")));

    // When
    Page<PokemonSummary> page = listPokemon.search(query("Pika"), new PageRequest(0, 20));

    // Then
    assertThat(page.content())
        .extracting(PokemonSummary::name)
        .containsExactly("pikachu", "pikachu-rock-star", "pikachu-belle");
    assertThat(page.totalElements()).isEqualTo(3);
    assertThat(page.page()).isZero();
    assertThat(page.size()).isEqualTo(20);
  }

  @Test
  void should_fetchOnlyTheRequestedPageOfMatches_when_theySpanSeveralPages() {
    // Given: "chu" matches pikachu, raichu, pichu, pikachu-rock-star and pikachu-belle
    given(catalog.findAllNames()).willReturn(NAMES);
    given(catalog.findSummaries(ids(172, 10080)))
        .willReturn(List.of(summary(172, "pichu"), summary(10080, "pikachu-rock-star")));

    // When
    Page<PokemonSummary> page = listPokemon.search(query("chu"), new PageRequest(1, 2));

    // Then
    assertThat(page.content())
        .extracting(PokemonSummary::name)
        .containsExactly("pichu", "pikachu-rock-star");
    assertThat(page.totalElements()).isEqualTo(5);
    assertThat(page.totalPages()).isEqualTo(3);
    then(catalog).should().findSummaries(ids(172, 10080));
  }

  @Test
  void should_fetchNoSummaries_when_nothingMatches() {
    // Given
    given(catalog.findAllNames()).willReturn(NAMES);

    // When
    Page<PokemonSummary> page = listPokemon.search(query("zzz"), new PageRequest(0, 20));

    // Then
    assertThat(page.content()).isEmpty();
    assertThat(page.totalElements()).isZero();
    then(catalog).should(never()).findSummaries(any());
  }

  @Test
  void should_fetchNoSummaries_when_thePageStartsAfterTheLastMatch() {
    // Given: "saur" matches 3 names
    given(catalog.findAllNames()).willReturn(NAMES);

    // When
    Page<PokemonSummary> page = listPokemon.search(query("saur"), new PageRequest(5, 20));

    // Then
    assertThat(page.content()).isEmpty();
    assertThat(page.totalElements()).isEqualTo(3);
    then(catalog).should(never()).findSummaries(any());
  }

  @Test
  void should_propagateUnavailability_when_theNameIndexCannotBeLoaded() {
    // Given
    given(catalog.findAllNames())
        .willThrow(new CatalogUnavailableException("PokeAPI unreachable", null));

    // When / Then
    assertThatThrownBy(() -> listPokemon.search(query("pika"), new PageRequest(0, 20)))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  private static PokemonNameQuery query(String raw) {
    return PokemonNameQuery.parse(raw).orElseThrow();
  }

  private static CatalogName name(int id, String name) {
    return new CatalogName(new PokemonId(id), name);
  }

  private static List<PokemonId> ids(int... ids) {
    return java.util.Arrays.stream(ids).mapToObj(PokemonId::new).toList();
  }

  private static PokemonSummary summary(int id, String name) {
    return new PokemonSummary(
        new PokemonId(id),
        name,
        "https://img/" + id + ".png",
        "Mouse Pokémon",
        Weight.ofHectograms(60),
        List.of("static"));
  }
}

package com.ballastlane.pokedex.adapter.in.web.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ballastlane.pokedex.application.port.in.pokemon.GetPokemonDetail;
import com.ballastlane.pokedex.application.port.in.pokemon.ListPokemon;
import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
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
import com.ballastlane.pokedex.support.WebSliceTest;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebSliceTest(controllers = PokemonController.class)
class PokemonControllerTest {

  private static final PokemonSummary BULBASAUR =
      new PokemonSummary(
          new PokemonId(1),
          "bulbasaur",
          "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
          "Seed Pokémon",
          Weight.ofHectograms(69),
          List.of("overgrow", "chlorophyll"));

  @Autowired private MockMvcTester mvc;
  @MockitoBean private ListPokemon listPokemon;
  @MockitoBean private GetPokemonDetail getPokemonDetail;

  @Test
  void should_returnTheEnvelopeWithCatalogEntries_when_pageIsRequested() {
    // Given
    PageRequest request = new PageRequest(0, 3);
    given(listPokemon.list(request)).willReturn(Page.of(List.of(BULBASAUR), request, 1351));

    // When (anonymous: the catalog is public)
    MvcTestResult result = mvc.get().uri("/api/v1/pokemon?page=0&size=3").exchange();

    // Then
    assertThat(result).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_JSON);
    assertThat(result)
        .bodyJson()
        .extractingPath("$")
        .asMap()
        .containsOnlyKeys("content", "page", "size", "totalElements", "totalPages");
    assertThat(result).bodyJson().extractingPath("$.page").asNumber().isEqualTo(0);
    assertThat(result).bodyJson().extractingPath("$.size").asNumber().isEqualTo(3);
    assertThat(result).bodyJson().extractingPath("$.totalElements").asNumber().isEqualTo(1351);
    assertThat(result).bodyJson().extractingPath("$.totalPages").asNumber().isEqualTo(451);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.content[0]")
        .asMap()
        .containsOnlyKeys("id", "name", "spriteUrl", "category", "weight", "abilities")
        .containsEntry("id", 1)
        .containsEntry("name", "bulbasaur")
        .containsEntry("category", "Seed Pokémon")
        .containsEntry("abilities", List.of("overgrow", "chlorophyll"));
    assertThat(result)
        .bodyJson()
        .extractingPath("$.content[0].weight")
        .asNumber()
        .satisfies(
            weight -> assertThat(new BigDecimal(weight.toString())).isEqualByComparingTo("6.9"));
  }

  @Test
  void should_useFirstPageOfTwenty_when_noParametersAreGiven() {
    // Given
    PageRequest defaults = new PageRequest(0, 20);
    given(listPokemon.list(defaults)).willReturn(Page.of(List.of(), defaults, 1351));

    // When
    MvcTestResult result = mvc.get().uri("/api/v1/pokemon").exchange();

    // Then
    assertThat(result).hasStatus(HttpStatus.OK);
    then(listPokemon).should().list(defaults);
  }

  @Test
  void should_keepNullCategoryAndSprite_when_sourceHasNone() {
    PageRequest request = new PageRequest(0, 1);
    PokemonSummary bare =
        new PokemonSummary(
            new PokemonId(10001), "deoxys-attack", null, null, Weight.ofHectograms(608), List.of());
    given(listPokemon.list(request)).willReturn(Page.of(List.of(bare), request, 1351));

    MvcTestResult result = mvc.get().uri("/api/v1/pokemon?size=1").exchange();

    assertThat(result)
        .bodyJson()
        .extractingPath("$.content[0]")
        .asMap()
        .containsEntry("spriteUrl", null)
        .containsEntry("category", null);
  }

  @Test
  void should_return400NamingSize_when_sizeIsAboveTheMaximum() {
    MvcTestResult result = mvc.get().uri("/api/v1/pokemon?size=101").exchange();

    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].field")
        .asArray()
        .containsExactly("size");
    then(listPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_return400NamingBothParameters_when_bothAreNegative() {
    MvcTestResult result = mvc.get().uri("/api/v1/pokemon?page=-1&size=-5").exchange();

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].field")
        .asArray()
        .containsExactly("page", "size");
  }

  @Test
  void should_return400NamingPage_when_pageIsNotANumber() {
    MvcTestResult result = mvc.get().uri("/api/v1/pokemon?page=abc").exchange();

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("page");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[0].message")
        .isEqualTo("has the wrong type");
  }

  @Test
  void should_return503WithRetryAfterAndNoInternals_when_catalogIsUnavailable() throws Exception {
    // Given
    given(listPokemon.list(new PageRequest(0, 20)))
        .willThrow(
            new CatalogUnavailableException(
                "The Pokémon catalog source is unavailable",
                new ConnectException("Connection refused: pokeapi.co/api/v2/pokemon")));

    // When
    MvcTestResult result = mvc.get().uri("/api/v1/pokemon").exchange();

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .hasHeader(HttpHeaders.RETRY_AFTER, "30");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.type")
        .asString()
        .endsWith("#service-unavailable");
    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("pokeapi.co")
        .doesNotContain("ConnectException")
        .doesNotContain("Connection refused");
  }

  // ---- detail (US02) -------------------------------------------------------------------------

  private static final PokemonDetail BULBASAUR_DETAIL =
      new PokemonDetail(
          new PokemonId(1),
          "bulbasaur",
          "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
          "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png",
          "Seed Pokémon",
          "While it is young, it uses the nutrients that are stored in the seed on its back.",
          List.of("grass", "poison"),
          Height.ofDecimetres(7),
          Weight.ofHectograms(69),
          List.of("overgrow", "chlorophyll"),
          new BaseStats(45, 49, 49, 65, 65, 45),
          new EvolutionStage(
              new PokemonId(1),
              "bulbasaur",
              null,
              List.of(
                  new EvolutionStage(
                      new PokemonId(2),
                      "ivysaur",
                      "Level 16",
                      List.of(
                          new EvolutionStage(
                              new PokemonId(3), "venusaur", "Level 32", List.of()))))));

  @Test
  void should_returnTheFullDetail_when_pokemonExists() {
    // Given
    given(getPokemonDetail.get(PokemonIdentifier.parse("1"))).willReturn(BULBASAUR_DETAIL);

    // When
    MvcTestResult result = mvc.get().uri("/api/v1/pokemon/1").exchange();

    // Then
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result)
        .bodyJson()
        .extractingPath("$")
        .asMap()
        .containsOnlyKeys(
            "id",
            "name",
            "imageUrl",
            "category",
            "description",
            "types",
            "height",
            "weight",
            "abilities",
            "stats",
            "evolutionChain");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.stats")
        .asMap()
        .containsOnlyKeys("hp", "attack", "defense", "specialAttack", "specialDefense", "speed")
        .containsEntry("specialAttack", 65);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.height")
        .asNumber()
        .satisfies(
            height -> assertThat(new BigDecimal(height.toString())).isEqualByComparingTo("0.7"));
    assertThat(result).bodyJson().extractingPath("$.evolutionChain.name").isEqualTo("bulbasaur");
    assertThat(result).bodyJson().extractingPath("$.evolutionChain.condition").isNull();
    assertThat(result)
        .bodyJson()
        .extractingPath("$.evolutionChain.evolvesTo[0].condition")
        .isEqualTo("Level 16");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.evolutionChain.evolvesTo[0].evolvesTo[0].evolvesTo")
        .asArray()
        .isEmpty();
  }

  @Test
  void should_return404NamingTheIdentifier_when_noPokemonMatches() {
    PokemonIdentifier missingno = PokemonIdentifier.parse("missingno");
    given(getPokemonDetail.get(missingno)).willThrow(new PokemonNotFoundException(missingno));

    MvcTestResult result = mvc.get().uri("/api/v1/pokemon/missingno").exchange();

    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(result).bodyJson().extractingPath("$.type").asString().endsWith("#not-found");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("No Pokémon matches 'missingno'.");
    then(getPokemonDetail).should().get(missingno);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "pika chu",
        "<script>",
        "0",
        "-1",
        "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxy"
      })
  void should_return400WithoutCallingPokeApi_when_identifierIsMalformed(String identifier) {
    MvcTestResult result = mvc.get().uri("/api/v1/pokemon/{idOrName}", identifier).exchange();

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("idOrName");
    then(getPokemonDetail).should(never()).get(any());
  }

  @Test
  void should_return503_when_catalogIsUnavailableForADetail() {
    given(getPokemonDetail.get(PokemonIdentifier.parse("1")))
        .willThrow(new CatalogUnavailableException("PokeAPI unreachable", null));

    MvcTestResult result = mvc.get().uri("/api/v1/pokemon/1").exchange();

    assertThat(result)
        .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
        .hasHeader(HttpHeaders.RETRY_AFTER, "30");
  }
}

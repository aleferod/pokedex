package com.ballastlane.pokedex.adapter.out.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.NamedResourceDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonSpeciesDto;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PokeApiMapperTest {

  @Test
  void should_mapEveryCatalogField_when_givenRealBulbasaurResponses() {
    // Given
    PokemonDto pokemon = PokeApiFixtures.read("pokemon-1.json", PokemonDto.class);
    PokemonSpeciesDto species = PokeApiFixtures.read("species-1.json", PokemonSpeciesDto.class);

    // When
    PokemonSummary summary = PokeApiMapper.toSummary(pokemon, species);

    // Then
    assertThat(summary.id()).isEqualTo(new PokemonId(1));
    assertThat(summary.name()).isEqualTo("bulbasaur");
    assertThat(summary.spriteUrl())
        .isEqualTo(
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
    assertThat(summary.category()).isEqualTo("Seed Pokémon");
    assertThat(summary.weight().kilograms()).isEqualTo(new BigDecimal("6.9"));
    assertThat(summary.abilities()).containsExactly("overgrow", "chlorophyll");
  }

  @Test
  void should_mapPikachu_when_givenRealPikachuResponses() {
    PokemonSummary summary =
        PokeApiMapper.toSummary(
            PokeApiFixtures.read("pokemon-25.json", PokemonDto.class),
            PokeApiFixtures.read("species-25.json", PokemonSpeciesDto.class));

    assertThat(summary.category()).isEqualTo("Mouse Pokémon");
    assertThat(summary.weight().kilograms()).isEqualTo(new BigDecimal("6.0"));
    assertThat(summary.abilities()).containsExactly("static", "lightning-rod");
  }

  @Test
  void should_leaveCategoryNull_when_noEnglishGenusExists() {
    PokemonSpeciesDto japaneseOnly =
        new PokemonSpeciesDto(
            1,
            "bulbasaur",
            List.of(new PokemonSpeciesDto.Genus("たねポケモン", new NamedResourceDto("ja", null))),
            List.of(),
            null);

    PokemonSummary summary =
        PokeApiMapper.toSummary(
            PokeApiFixtures.read("pokemon-1.json", PokemonDto.class), japaneseOnly);

    assertThat(summary.category()).isNull();
  }

  @Test
  void should_leaveSpriteNull_when_noDefaultSpriteExists() {
    PokemonDto noSprite =
        new PokemonDto(
            1,
            "bulbasaur",
            69,
            new PokemonDto.Sprites(null, null),
            List.of(),
            new NamedResourceDto("bulbasaur", "https://pokeapi.co/api/v2/pokemon-species/1/"),
            7,
            List.of(),
            List.of());

    PokemonSummary summary =
        PokeApiMapper.toSummary(
            noSprite, PokeApiFixtures.read("species-1.json", PokemonSpeciesDto.class));

    assertThat(summary.spriteUrl()).isNull();
    assertThat(summary.abilities()).isEmpty();
  }

  @Test
  void should_orderAbilitiesBySlot_when_upstreamListsThemOutOfOrder() {
    PokemonDto shuffled =
        new PokemonDto(
            25,
            "pikachu",
            60,
            new PokemonDto.Sprites("https://img/25.png", null),
            List.of(
                new PokemonDto.AbilitySlot(3, true, new NamedResourceDto("lightning-rod", null)),
                new PokemonDto.AbilitySlot(1, false, new NamedResourceDto("static", null))),
            new NamedResourceDto("pikachu", "https://pokeapi.co/api/v2/pokemon-species/25/"),
            4,
            List.of(),
            List.of());

    PokemonSummary summary =
        PokeApiMapper.toSummary(
            shuffled, PokeApiFixtures.read("species-25.json", PokemonSpeciesDto.class));

    assertThat(summary.abilities()).containsExactly("static", "lightning-rod");
  }

  @ParameterizedTest
  @CsvSource({
    "https://pokeapi.co/api/v2/pokemon/25/, 25",
    "https://pokeapi.co/api/v2/pokemon-species/1, 1",
    "https://pokeapi.co/api/v2/pokemon/10001/, 10001"
  })
  void should_extractTheId_when_givenAResourceUrl(String url, int id) {
    assertThat(PokeApiMapper.idFromUrl(url)).isEqualTo(id);
  }

  @Test
  void should_reject_when_urlHasNoId() {
    assertThatThrownBy(() -> PokeApiMapper.idFromUrl("https://pokeapi.co/api/v2/pokemon/"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

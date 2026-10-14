package com.ballastlane.pokedex.adapter.out.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionChainDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.NamedResourceDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.PokemonSpeciesDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.evolution.EvolutionConditions;
import com.ballastlane.pokedex.domain.pokemon.BaseStats;
import com.ballastlane.pokedex.domain.pokemon.EvolutionStage;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Detail mapping on real PokeAPI captures (Bulbasaur, Eevee, Pikachu, Ditto). */
class PokeApiDetailMapperTest {

  private final PokeApiDetailMapper mapper = new PokeApiDetailMapper(new EvolutionConditions());

  @Test
  void should_mapEveryDetailField_when_givenBulbasaur() {
    // When
    PokemonDetail bulbasaur = detail(1, 1, 1);

    // Then
    assertThat(bulbasaur.id()).isEqualTo(new PokemonId(1));
    assertThat(bulbasaur.name()).isEqualTo("bulbasaur");
    assertThat(bulbasaur.imageUrl()).endsWith("/official-artwork/1.png");
    assertThat(bulbasaur.category()).isEqualTo("Seed Pokémon");
    assertThat(bulbasaur.description())
        .isEqualTo(
            "While it is young, it uses the nutrients that are stored in the seed on its back"
                + " in order to grow.");
    assertThat(bulbasaur.types()).containsExactly("grass", "poison");
    assertThat(bulbasaur.height().metres()).isEqualTo(new BigDecimal("0.7"));
    assertThat(bulbasaur.weight().kilograms()).isEqualTo(new BigDecimal("6.9"));
    assertThat(bulbasaur.abilities()).containsExactly("overgrow", "chlorophyll");
    assertThat(bulbasaur.stats()).isEqualTo(new BaseStats(45, 49, 49, 65, 65, 45));
  }

  @Test
  void should_buildTheLevelBasedChain_when_givenBulbasaur() {
    EvolutionStage root = detail(1, 1, 1).evolutionChain();

    assertThat(root.name()).isEqualTo("bulbasaur");
    assertThat(root.condition()).isNull();
    EvolutionStage ivysaur = root.evolvesTo().getFirst();
    assertThat(ivysaur.id()).isEqualTo(new PokemonId(2));
    assertThat(ivysaur.condition()).isEqualTo("Level 16");
    EvolutionStage venusaur = ivysaur.evolvesTo().getFirst();
    assertThat(venusaur.name()).isEqualTo("venusaur");
    assertThat(venusaur.condition()).isEqualTo("Level 32");
    assertThat(venusaur.evolvesTo()).isEmpty();
  }

  @Test
  void should_keepAllEightBranches_when_givenEevee() {
    EvolutionStage eevee = detail(133, 133, 67).evolutionChain();

    assertThat(eevee.name()).isEqualTo("eevee");
    assertThat(eevee.evolvesTo())
        .extracting(EvolutionStage::name)
        .containsExactly(
            "vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon");
    assertThat(eevee.evolvesTo())
        .extracting(EvolutionStage::condition)
        .containsExactly(
            "Use Water Stone",
            "Use Thunder Stone",
            "Use Fire Stone",
            "Level up with high friendship during the day",
            "Level up with high friendship at night",
            "Use Leaf Stone",
            "Use Ice Stone",
            "Level up with high affection knowing a Fairy move");
    assertThat(eevee.evolvesTo().getLast().id()).isEqualTo(new PokemonId(700));
  }

  @Test
  void should_startFromTheBaseForm_when_givenAMiddleStage() {
    EvolutionStage pichu = detail(25, 25, 10).evolutionChain();

    assertThat(pichu.name()).isEqualTo("pichu");
    assertThat(pichu.id()).isEqualTo(new PokemonId(172));
    EvolutionStage pikachu = pichu.evolvesTo().getFirst();
    assertThat(pikachu.condition()).isEqualTo("Level up with high friendship");
    assertThat(pikachu.evolvesTo())
        .singleElement()
        .satisfies(
            raichu -> {
              assertThat(raichu.name()).isEqualTo("raichu");
              assertThat(raichu.condition()).isEqualTo("Use Thunder Stone");
            });
  }

  @Test
  void should_haveASingleStage_when_givenDitto() {
    PokemonDetail ditto = detail(132, 132, 66);

    assertThat(ditto.evolutionChain().name()).isEqualTo("ditto");
    assertThat(ditto.evolutionChain().evolvesTo()).isEmpty();
    assertThat(ditto.description()).isNotBlank().doesNotContain("\n");
  }

  @Test
  void should_leaveDescriptionNull_when_noEnglishFlavorTextExists() {
    PokemonSpeciesDto frenchOnly =
        new PokemonSpeciesDto(
            1,
            "bulbasaur",
            List.of(),
            List.of(
                new PokemonSpeciesDto.FlavorTextEntry(
                    "Une graine étrange.", new NamedResourceDto("fr", null))),
            new NamedResourceDto(null, "https://pokeapi.co/api/v2/evolution-chain/1/"));

    PokemonDetail detail =
        mapper.toDetail(
            PokeApiFixtures.read("pokemon-1.json", PokemonDto.class),
            frenchOnly,
            PokeApiFixtures.read("evolution-chain-1.json", EvolutionChainDto.class));

    assertThat(detail.description()).isNull();
    assertThat(detail.category()).isNull();
  }

  private PokemonDetail detail(int pokemon, int species, int chain) {
    return mapper.toDetail(
        PokeApiFixtures.read("pokemon-" + pokemon + ".json", PokemonDto.class),
        PokeApiFixtures.read("species-" + species + ".json", PokemonSpeciesDto.class),
        PokeApiFixtures.read("evolution-chain-" + chain + ".json", EvolutionChainDto.class));
  }
}

package com.ballastlane.pokedex.adapter.out.pokeapi.evolution;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballastlane.pokedex.adapter.out.pokeapi.dto.EvolutionDetailDto;
import com.ballastlane.pokedex.adapter.out.pokeapi.dto.NamedResourceDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvolutionConditionsTest {

  private final EvolutionConditions conditions = new EvolutionConditions();

  @Test
  void should_haveNoCondition_when_stageIsTheBaseForm() {
    assertThat(conditions.describe(List.of())).isNull();
  }

  @Test
  void should_sayTheLevel_when_evolutionIsLevelBased() {
    assertThat(conditions.describe(List.of(detail("level-up").minLevel(16).build())))
        .isEqualTo("Level 16");
  }

  @Test
  void should_describeFriendshipAndTimeOfDay_when_levelUpHasNoLevel() {
    assertThat(
            conditions.describe(
                List.of(detail("level-up").minHappiness(160).timeOfDay("day").build())))
        .isEqualTo("Level up with high friendship during the day");
    assertThat(
            conditions.describe(
                List.of(detail("level-up").minHappiness(160).timeOfDay("night").build())))
        .isEqualTo("Level up with high friendship at night");
    assertThat(conditions.describe(List.of(detail("level-up").minHappiness(220).build())))
        .isEqualTo("Level up with high friendship");
  }

  @Test
  void should_describeAffectionAndMoveType_when_sylveonStyleEvolution() {
    assertThat(
            conditions.describe(
                List.of(detail("level-up").minAffection(2).knownMoveType("fairy").build())))
        .isEqualTo("Level up with high affection knowing a Fairy move");
  }

  @Test
  void should_combineLevelWithQualifiers_when_bothArePresent() {
    assertThat(
            conditions.describe(
                List.of(
                    detail("level-up")
                        .minLevel(25)
                        .timeOfDay("day")
                        .heldItem("oval-stone")
                        .build())))
        .isEqualTo("Level 25 during the day holding Oval Stone");
  }

  @Test
  void should_nameTheItem_when_evolutionUsesAnItem() {
    assertThat(conditions.describe(List.of(detail("use-item").item("water-stone").build())))
        .isEqualTo("Use Water Stone");
  }

  @Test
  void should_describeTrades_when_evolutionNeedsATrade() {
    assertThat(conditions.describe(List.of(detail("trade").build()))).isEqualTo("Trade");
    assertThat(conditions.describe(List.of(detail("trade").heldItem("metal-coat").build())))
        .isEqualTo("Trade holding Metal Coat");
    assertThat(conditions.describe(List.of(detail("trade").tradeSpecies("shelmet").build())))
        .isEqualTo("Trade for Shelmet");
  }

  @Test
  void should_humanizeTheTrigger_when_noSpecificStrategyExists() {
    assertThat(conditions.describe(List.of(detail("tower-of-darkness").build())))
        .isEqualTo("Tower Of Darkness");
  }

  @Test
  void should_preferALocationFreeMethod_when_someMethodsAreLocationSpecific() {
    assertThat(
            conditions.describe(
                List.of(
                    detail("level-up").location("eterna-forest").build(),
                    detail("level-up").location("pinwheel-forest").build(),
                    detail("use-item").item("leaf-stone").build(),
                    detail("level-up").location("lush-jungle").build())))
        .isEqualTo("Use Leaf Stone");
  }

  @Test
  void should_describeTheLocation_when_everyMethodIsLocationSpecific() {
    assertThat(
            conditions.describe(
                List.of(
                    detail("level-up").location("mt-coronet").build(),
                    detail("level-up").location("chargestone-cave").build())))
        .isEqualTo("Level up at Mt Coronet");
  }

  private static Builder detail(String trigger) {
    return new Builder(trigger);
  }

  /** Test data builder for PokeAPI evolution details. */
  private static final class Builder {
    private final String trigger;
    private Integer minLevel;
    private String item;
    private String heldItem;
    private Integer minHappiness;
    private Integer minAffection;
    private String timeOfDay = "";
    private String knownMoveType;
    private String location;
    private String tradeSpecies;

    Builder(String trigger) {
      this.trigger = trigger;
    }

    Builder minLevel(int value) {
      minLevel = value;
      return this;
    }

    Builder item(String value) {
      item = value;
      return this;
    }

    Builder heldItem(String value) {
      heldItem = value;
      return this;
    }

    Builder minHappiness(int value) {
      minHappiness = value;
      return this;
    }

    Builder minAffection(int value) {
      minAffection = value;
      return this;
    }

    Builder timeOfDay(String value) {
      timeOfDay = value;
      return this;
    }

    Builder knownMoveType(String value) {
      knownMoveType = value;
      return this;
    }

    Builder location(String value) {
      location = value;
      return this;
    }

    Builder tradeSpecies(String value) {
      tradeSpecies = value;
      return this;
    }

    EvolutionDetailDto build() {
      return new EvolutionDetailDto(
          named(trigger),
          minLevel,
          named(item),
          named(heldItem),
          minHappiness,
          minAffection,
          timeOfDay,
          named(knownMoveType),
          null,
          named(location),
          named(tradeSpecies));
    }

    private static NamedResourceDto named(String name) {
      return name == null ? null : new NamedResourceDto(name, null);
    }
  }
}

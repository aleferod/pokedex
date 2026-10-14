package com.ballastlane.pokedex.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import org.junit.jupiter.api.Test;

class BaseStatsTest {

  @Test
  void should_keepEveryStat_when_valid() {
    BaseStats bulbasaur = new BaseStats(45, 49, 49, 65, 65, 45);

    assertThat(bulbasaur.hp()).isEqualTo(45);
    assertThat(bulbasaur.specialAttack()).isEqualTo(65);
    assertThat(bulbasaur.speed()).isEqualTo(45);
  }

  @Test
  void should_rejectNamingTheStat_when_aStatIsNegative() {
    assertThatThrownBy(() -> new BaseStats(45, -1, 49, 65, 65, 45))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> assertThat(error.field()).isEqualTo("stats.attack"));
  }
}

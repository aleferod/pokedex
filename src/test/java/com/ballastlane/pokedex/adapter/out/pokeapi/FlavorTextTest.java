package com.ballastlane.pokedex.adapter.out.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FlavorTextTest {

  @Test
  void should_replaceLineBreaksWithSpaces_when_textIsWrapped() {
    assertThat(
            FlavorText.clean(
                "While it is young, it uses the nutrients that are\n"
                    + "stored in the seed on its back in order to grow."))
        .isEqualTo(
            "While it is young, it uses the nutrients that are stored in the seed on its back"
                + " in order to grow.");
  }

  @Test
  void should_replaceFormFeedsAndCollapseWhitespace_when_textComesFromOldGames() {
    assertThat(
            FlavorText.clean(
                "A strange seed was\nplanted on its\nback at birth.\fThe plant sprouts\n"
                    + "and grows with\nthis POKéMON."))
        .isEqualTo(
            "A strange seed was planted on its back at birth. The plant sprouts and grows"
                + " with this POKéMON.");
  }

  @Test
  void should_dropSoftHyphensAndOtherControls_when_present() {
    assertThat(FlavorText.clean("  It is a­\nvery\r\n\tcalm\u0007 POKéMON.  "))
        .isEqualTo("It is a very calm POKéMON.");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  \n\f "})
  void should_returnNull_when_nothingReadableRemains(String raw) {
    assertThat(FlavorText.clean(raw)).isNull();
  }
}

package com.ballastlane.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PageTest {

  @ParameterizedTest
  @CsvSource({"1351, 3, 451", "1351, 20, 68", "1340, 20, 67", "0, 20, 0", "1, 100, 1"})
  void should_roundTotalPagesUp_when_lastPageIsPartial(long total, int size, int pages) {
    assertThat(Page.of(List.of(), new PageRequest(0, size), total).totalPages()).isEqualTo(pages);
  }

  @Test
  void should_keepRequestAndContent_when_built() {
    Page<String> page = Page.of(List.of("bulbasaur", "ivysaur"), new PageRequest(1, 2), 1351);

    assertThat(page.content()).containsExactly("bulbasaur", "ivysaur");
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(2);
    assertThat(page.totalElements()).isEqualTo(1351);
  }

  @Test
  void should_beImmutable_when_sourceListChangesLater() {
    List<String> source = new ArrayList<>(List.of("bulbasaur"));
    Page<String> page = Page.of(source, new PageRequest(0, 20), 1);

    source.add("ivysaur");

    assertThat(page.content()).containsExactly("bulbasaur");
    assertThatThrownBy(() -> page.content().add("x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void should_reject_when_totalIsNegative() {
    assertThatThrownBy(() -> Page.of(List.of(), new PageRequest(0, 20), -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

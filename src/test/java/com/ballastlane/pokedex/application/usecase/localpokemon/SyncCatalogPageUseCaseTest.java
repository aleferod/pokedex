package com.ballastlane.pokedex.application.usecase.localpokemon;

import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ballastlane.pokedex.application.port.in.localpokemon.BulkSyncResult;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.SyncResult;
import com.ballastlane.pokedex.application.port.in.pokemon.ListPokemon;
import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonSummary;
import com.ballastlane.pokedex.domain.pokemon.Weight;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.shared.Page;
import com.ballastlane.pokedex.domain.shared.PageRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncCatalogPageUseCaseTest {

  @Mock private ListPokemon listPokemon;
  @Mock private SyncPokemon syncPokemon;
  @InjectMocks private SyncCatalogPageUseCase syncCatalogPage;

  @Test
  void should_syncEachEntryInCatalogOrderAndCountOutcomes_when_limitIsValid() {
    // Given
    given(listPokemon.list(new PageRequest(0, 3)))
        .willReturn(
            Page.of(
                List.of(summary(1, "bulbasaur"), summary(2, "ivysaur"), summary(3, "venusaur")),
                new PageRequest(0, 3),
                1351));
    given(syncPokemon.sync(byId(1), "ash")).willReturn(result(true));
    given(syncPokemon.sync(byId(2), "ash")).willReturn(result(false));
    given(syncPokemon.sync(byId(3), "ash")).willReturn(result(true));

    // When
    BulkSyncResult bulk = syncCatalogPage.syncFirst(3, "ash");

    // Then
    assertThat(bulk.results())
        .extracting(BulkSyncResult.Entry::name)
        .containsExactly("bulbasaur", "ivysaur", "venusaur");
    assertThat(bulk.results())
        .extracting(BulkSyncResult.Entry::created)
        .containsExactly(true, false, true);
    assertThat(bulk.created()).isEqualTo(2);
    assertThat(bulk.updated()).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 21})
  void should_rejectNamingLimit_when_outsideOneToTwenty(int limit) {
    assertThatThrownBy(() -> syncCatalogPage.syncFirst(limit, "ash"))
        .isInstanceOfSatisfying(
            InvalidFieldException.class, error -> assertThat(error.field()).isEqualTo("limit"));
    then(listPokemon).shouldHaveNoInteractions();
  }

  @Test
  void should_stopAtTheFirstFailure_when_anEntryCannotBeSynced() {
    // Given
    given(listPokemon.list(new PageRequest(0, 3)))
        .willReturn(
            Page.of(
                List.of(summary(1, "bulbasaur"), summary(2, "ivysaur"), summary(3, "venusaur")),
                new PageRequest(0, 3),
                1351));
    given(syncPokemon.sync(byId(1), "ash")).willReturn(result(true));
    given(syncPokemon.sync(byId(2), "ash"))
        .willThrow(new CatalogUnavailableException("PokeAPI unreachable", null));

    // When / Then
    assertThatThrownBy(() -> syncCatalogPage.syncFirst(3, "ash"))
        .isInstanceOf(CatalogUnavailableException.class);
    then(syncPokemon).should(never()).sync(byId(3), "ash");
  }

  private static PokemonIdentifier byId(int id) {
    return new PokemonIdentifier.ById(new PokemonId(id));
  }

  private static PokemonSummary summary(int id, String name) {
    return new PokemonSummary(
        new PokemonId(id), name, null, "Seed Pokémon", Weight.ofHectograms(69), List.of());
  }

  private static SyncResult result(boolean created) {
    return new SyncResult(
        LocalPokemon.importFrom(
            LocalPokemonId.random(), new PokemonId(25), pikachu(60), "ash", Instant.EPOCH),
        created);
  }
}

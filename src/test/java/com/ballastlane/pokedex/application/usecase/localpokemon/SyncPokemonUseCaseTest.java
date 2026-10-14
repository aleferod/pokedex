package com.ballastlane.pokedex.application.usecase.localpokemon;

import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.ballastlane.pokedex.application.port.in.localpokemon.SyncResult;
import com.ballastlane.pokedex.application.port.in.pokemon.GetPokemonDetail;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.domain.localpokemon.AuditInfo;
import com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.InternalTags;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemonId;
import com.ballastlane.pokedex.domain.localpokemon.LocalizedNames;
import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import com.ballastlane.pokedex.domain.localpokemon.StaleLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.UpstreamData;
import com.ballastlane.pokedex.domain.pokemon.EvolutionStage;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncPokemonUseCaseTest {

  private static final Instant IMPORTED = Instant.parse("2026-10-14T12:00:00Z");
  private static final Instant NOW = Instant.parse("2026-10-15T09:00:00Z");
  private static final PokemonIdentifier PIKACHU = PokemonIdentifier.parse("pikachu");
  private static final PokemonId NUMBER_25 = new PokemonId(25);

  @Mock private GetPokemonDetail getPokemonDetail;
  @Mock private LocalPokemonRepositoryPort repository;

  private SyncPokemonUseCase syncPokemon;

  @BeforeEach
  void setUp() {
    syncPokemon =
        new SyncPokemonUseCase(getPokemonDetail, repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void should_importWithEmptyProprietaryData_when_notStoredYet() {
    // Given
    given(getPokemonDetail.get(PIKACHU)).willReturn(detail(61));
    given(repository.findByPokeapiId(NUMBER_25)).willReturn(Optional.empty());
    given(repository.save(any())).willAnswer(invocation -> stored(invocation.getArgument(0), 0L));

    // When
    SyncResult result = syncPokemon.sync(PIKACHU, "ash");

    // Then
    assertThat(result.created()).isTrue();
    LocalPokemon saved = savedPokemon();
    assertThat(saved.pokeapiId()).isEqualTo(NUMBER_25);
    assertThat(saved.upstream().weight().hectograms()).isEqualTo(61);
    assertThat(saved.proprietary()).isEqualTo(ProprietaryData.empty());
    assertThat(saved.audit()).isEqualTo(new AuditInfo(NOW, NOW, NOW, "ash"));
    assertThat(saved.version()).isNull();
    assertThat(result.pokemon().version()).isZero();
  }

  @Test
  void should_refreshUpstreamAndKeepProprietaryData_when_alreadyStored() {
    // Given
    LocalPokemon existing = existingWithProprietaryData(3L);
    given(getPokemonDetail.get(PIKACHU)).willReturn(detail(61));
    given(repository.findByPokeapiId(NUMBER_25)).willReturn(Optional.of(existing));
    given(repository.save(any())).willAnswer(invocation -> stored(invocation.getArgument(0), 4L));

    // When
    SyncResult result = syncPokemon.sync(PIKACHU, "misty");

    // Then
    assertThat(result.created()).isFalse();
    LocalPokemon saved = savedPokemon();
    assertThat(saved.upstream().weight().hectograms()).isEqualTo(61);
    assertThat(saved.proprietary()).isEqualTo(existing.proprietary());
    assertThat(saved.audit()).isEqualTo(new AuditInfo(IMPORTED, NOW, NOW, "ash"));
    assertThat(saved.version()).isEqualTo(3L);
  }

  @Test
  void should_touchNothing_when_pokemonIsUnknown() {
    PokemonIdentifier missingno = PokemonIdentifier.parse("missingno");
    given(getPokemonDetail.get(missingno)).willThrow(new PokemonNotFoundException(missingno));

    assertThatThrownBy(() -> syncPokemon.sync(missingno, "ash"))
        .isInstanceOf(PokemonNotFoundException.class);
    then(repository).shouldHaveNoInteractions();
  }

  @Test
  void should_touchNothing_when_pokeApiIsUnavailable() {
    given(getPokemonDetail.get(PIKACHU))
        .willThrow(new CatalogUnavailableException("PokeAPI unreachable", null));

    assertThatThrownBy(() -> syncPokemon.sync(PIKACHU, "ash"))
        .isInstanceOf(CatalogUnavailableException.class);
    then(repository).shouldHaveNoInteractions();
  }

  @Test
  void should_resyncTheWinner_when_aConcurrentFirstImportWonTheRace() {
    // Given the insert loses against a concurrent first import
    LocalPokemon winner = existingWithProprietaryData(0L);
    given(getPokemonDetail.get(PIKACHU)).willReturn(detail(61));
    given(repository.findByPokeapiId(NUMBER_25))
        .willReturn(Optional.empty())
        .willReturn(Optional.of(winner));
    given(repository.save(any()))
        .willThrow(new DuplicateLocalPokemonException(NUMBER_25))
        .willAnswer(invocation -> stored(invocation.getArgument(0), 1L));

    // When
    SyncResult result = syncPokemon.sync(PIKACHU, "misty");

    // Then
    assertThat(result.created()).isFalse();
    assertThat(result.pokemon().id()).isEqualTo(winner.id());
    then(repository).should(times(2)).save(any());
  }

  @Test
  void should_retryOnce_when_aConcurrentResyncChangedTheRecord() {
    // Given
    LocalPokemon readFirst = existingWithProprietaryData(3L);
    LocalPokemon readAgain = existingWithProprietaryData(4L);
    given(getPokemonDetail.get(PIKACHU)).willReturn(detail(61));
    given(repository.findByPokeapiId(NUMBER_25))
        .willReturn(Optional.of(readFirst))
        .willReturn(Optional.of(readAgain));
    given(repository.save(any()))
        .willThrow(new StaleLocalPokemonException(readFirst.id()))
        .willAnswer(invocation -> stored(invocation.getArgument(0), 5L));

    // When
    SyncResult result = syncPokemon.sync(PIKACHU, "misty");

    // Then
    assertThat(result.pokemon().version()).isEqualTo(5L);
    assertThat(result.created()).isFalse();
  }

  @Test
  void should_giveUp_when_theRetryIsStaleToo() {
    LocalPokemon existing = existingWithProprietaryData(3L);
    given(getPokemonDetail.get(PIKACHU)).willReturn(detail(61));
    given(repository.findByPokeapiId(NUMBER_25)).willReturn(Optional.of(existing));
    given(repository.save(any())).willThrow(new StaleLocalPokemonException(existing.id()));

    assertThatThrownBy(() -> syncPokemon.sync(PIKACHU, "misty"))
        .isInstanceOf(StaleLocalPokemonException.class);
    then(repository).should(times(2)).save(any());
    then(getPokemonDetail).should(times(1)).get(PIKACHU);
    then(repository).should(never()).findById(any());
  }

  private LocalPokemon savedPokemon() {
    ArgumentCaptor<LocalPokemon> saved = ArgumentCaptor.forClass(LocalPokemon.class);
    then(repository).should(atLeastOnce()).save(saved.capture());
    return saved.getValue();
  }

  private static LocalPokemon stored(LocalPokemon pokemon, long version) {
    return new LocalPokemon(
        pokemon.id(),
        pokemon.pokeapiId(),
        pokemon.upstream(),
        pokemon.proprietary(),
        pokemon.audit(),
        version);
  }

  private static LocalPokemon existingWithProprietaryData(long version) {
    return new LocalPokemon(
        LocalPokemonId.random(),
        NUMBER_25,
        pikachu(60),
        new ProprietaryData(
            LocalizedNames.of(Map.of("pt-BR", "Pikachu")),
            "Kanto",
            null,
            InternalTags.of(List.of("starter-line")),
            null),
        new AuditInfo(IMPORTED, IMPORTED, IMPORTED, "ash"),
        version);
  }

  private static PokemonDetail detail(int weightHectograms) {
    UpstreamData upstream = pikachu(weightHectograms);
    return new PokemonDetail(
        NUMBER_25,
        upstream.name(),
        upstream.spriteUrl(),
        upstream.imageUrl(),
        upstream.category(),
        "It stores electricity in its cheeks.",
        upstream.types(),
        upstream.height(),
        upstream.weight(),
        upstream.abilities(),
        upstream.stats(),
        new EvolutionStage(new PokemonId(172), "pichu", null, List.of()));
  }
}

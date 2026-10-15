package com.ballastlane.pokedex.application.usecase.localpokemon;

import static com.ballastlane.pokedex.application.usecase.localpokemon.LocalPokemonTestData.NOW;
import static com.ballastlane.pokedex.application.usecase.localpokemon.LocalPokemonTestData.withVersion;
import static com.ballastlane.pokedex.domain.localpokemon.LocalPokemonFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemonCommand;
import com.ballastlane.pokedex.application.port.in.localpokemon.ProprietaryValues;
import com.ballastlane.pokedex.application.port.in.pokemon.GetPokemonDetail;
import com.ballastlane.pokedex.application.port.out.localpokemon.LocalPokemonRepositoryPort;
import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.domain.localpokemon.AuditInfo;
import com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException;
import com.ballastlane.pokedex.domain.localpokemon.LocalPokemon;
import com.ballastlane.pokedex.domain.localpokemon.UpstreamData;
import com.ballastlane.pokedex.domain.pokemon.EvolutionStage;
import com.ballastlane.pokedex.domain.pokemon.PokemonDetail;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.pokemon.PokemonIdentifier;
import com.ballastlane.pokedex.domain.pokemon.PokemonNotFoundException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldException;
import com.ballastlane.pokedex.domain.shared.InvalidFieldsException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateLocalPokemonUseCaseTest {

  private static final PokemonId NUMBER_25 = new PokemonId(25);
  private static final PokemonIdentifier BY_NUMBER_25 = new PokemonIdentifier.ById(NUMBER_25);
  private static final ProprietaryValues KANTO_STARTER =
      new ProprietaryValues(
          Map.of("pt-BR", " Pikachu "), " Kanto ", null, List.of("Starter", "starter"), null);

  @Mock private GetPokemonDetail getPokemonDetail;
  @Mock private LocalPokemonRepositoryPort repository;

  private CreateLocalPokemonUseCase createLocalPokemon;

  @BeforeEach
  void setUp() {
    createLocalPokemon =
        new CreateLocalPokemonUseCase(
            getPokemonDetail, repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void should_storeUpstreamAndNormalizedProprietaryData_when_numberIsNew() {
    // Given
    given(repository.existsByPokeapiId(NUMBER_25)).willReturn(false);
    given(getPokemonDetail.getFresh(BY_NUMBER_25)).willReturn(detail());
    given(repository.save(any()))
        .willAnswer(invocation -> withVersion(invocation.getArgument(0), 0));

    // When
    LocalPokemon created =
        createLocalPokemon.create(new CreateLocalPokemonCommand(25, KANTO_STARTER), "ash");

    // Then
    ArgumentCaptor<LocalPokemon> saved = ArgumentCaptor.forClass(LocalPokemon.class);
    then(repository).should().save(saved.capture());
    LocalPokemon toStore = saved.getValue();
    assertThat(toStore.pokeapiId()).isEqualTo(NUMBER_25);
    assertThat(toStore.upstream()).isEqualTo(pikachu(60));
    assertThat(toStore.proprietary().region()).isEqualTo("Kanto");
    assertThat(toStore.proprietary().internalTags().tags()).containsExactly("starter");
    assertThat(toStore.proprietary().localizedNames().names())
        .containsExactly(Map.entry("pt-BR", "Pikachu"));
    assertThat(toStore.audit()).isEqualTo(new AuditInfo(NOW, NOW, NOW, "ash"));
    assertThat(toStore.version()).isNull();
    assertThat(created.version()).isZero();
  }

  @Test
  void should_reportEveryInvalidField_before_anyLookup() {
    ProprietaryValues invalid =
        new ProprietaryValues(null, "x".repeat(51), null, List.of("Bad Tag!"), null);

    assertThatThrownBy(
            () -> createLocalPokemon.create(new CreateLocalPokemonCommand(0, invalid), "ash"))
        .isInstanceOfSatisfying(
            InvalidFieldsException.class,
            error ->
                assertThat(error.violations())
                    .extracting(InvalidFieldException::field)
                    .containsExactlyInAnyOrder("pokeapiId", "region", "internalTags"));
    verifyNoInteractions(repository, getPokemonDetail);
  }

  @Test
  void should_rejectAsDuplicate_before_callingPokeApi() {
    given(repository.existsByPokeapiId(NUMBER_25)).willReturn(true);

    assertThatThrownBy(
            () ->
                createLocalPokemon.create(new CreateLocalPokemonCommand(25, KANTO_STARTER), "ash"))
        .isInstanceOf(DuplicateLocalPokemonException.class);
    verifyNoInteractions(getPokemonDetail);
    then(repository).should(never()).save(any());
  }

  @Test
  void should_reportPokeapiIdAsInvalid_when_pokeApiHasNoSuchNumber() {
    given(repository.existsByPokeapiId(NUMBER_25)).willReturn(false);
    given(getPokemonDetail.getFresh(BY_NUMBER_25))
        .willThrow(new PokemonNotFoundException(BY_NUMBER_25));

    assertThatThrownBy(
            () ->
                createLocalPokemon.create(new CreateLocalPokemonCommand(25, KANTO_STARTER), "ash"))
        .isInstanceOfSatisfying(
            InvalidFieldException.class,
            error -> {
              assertThat(error.field()).isEqualTo("pokeapiId");
              assertThat(error.reason()).isEqualTo("no Pokémon has this number");
            });
    then(repository).should(never()).save(any());
  }

  @Test
  void should_storeNothing_when_pokeApiIsUnavailable() {
    given(repository.existsByPokeapiId(NUMBER_25)).willReturn(false);
    given(getPokemonDetail.getFresh(BY_NUMBER_25))
        .willThrow(new CatalogUnavailableException("down", null));

    assertThatThrownBy(
            () ->
                createLocalPokemon.create(new CreateLocalPokemonCommand(25, KANTO_STARTER), "ash"))
        .isInstanceOf(CatalogUnavailableException.class);
    then(repository).should(never()).save(any());
  }

  private static PokemonDetail detail() {
    UpstreamData upstream = pikachu(60);
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

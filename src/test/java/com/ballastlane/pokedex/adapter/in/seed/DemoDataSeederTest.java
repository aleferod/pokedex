package com.ballastlane.pokedex.adapter.in.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemonCommand;
import com.ballastlane.pokedex.application.port.in.localpokemon.ProprietaryValues;
import com.ballastlane.pokedex.application.port.in.user.ProvisionUser;
import com.ballastlane.pokedex.application.port.in.user.ProvisionUserCommand;
import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException;
import com.ballastlane.pokedex.domain.pokemon.PokemonId;
import com.ballastlane.pokedex.domain.user.DuplicateUserException;
import com.ballastlane.pokedex.domain.user.Role;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class DemoDataSeederTest {

  private static final DemoData.Account ADMIN =
      new DemoData.Account("admin", "admin@pokedex.example", "Admin@123", Role.ADMIN);
  private static final DemoData.Account TRAINER =
      new DemoData.Account("trainer", "trainer@pokedex.example", "Trainer@123", Role.USER);
  private static final DemoData.LocalPokemon BULBASAUR =
      new DemoData.LocalPokemon(
          1, Map.of("fr", "Bulbizarre"), "Kanto", "Grassland", List.of("starter"), "Seed starter");
  private static final DemoData.LocalPokemon PIKACHU =
      new DemoData.LocalPokemon(25, Map.of("ja", "ピカチュウ"), "Kanto", null, List.of("mascot"), null);

  private final ProvisionUser provisionUser = mock(ProvisionUser.class);
  private final CreateLocalPokemon createLocalPokemon = mock(CreateLocalPokemon.class);
  private final DemoDataSeeder seeder =
      new DemoDataSeeder(
          new DemoData("admin", List.of(ADMIN, TRAINER), List.of(BULBASAUR, PIKACHU)),
          provisionUser,
          createLocalPokemon);

  @Test
  void should_provisionEveryAccountAndCreateEveryPokemon_when_nothingExistsYet() {
    // Given
    given(provisionUser.provision(any())).willReturn(true);

    // When
    DemoDataSeeder.Result result = seeder.seed();

    // Then
    assertThat(result).isEqualTo(new DemoDataSeeder.Result(2, 0, 0, 2, 0, 0));
    then(provisionUser)
        .should()
        .provision(
            new ProvisionUserCommand("admin", "admin@pokedex.example", "Admin@123", Role.ADMIN));
    then(provisionUser)
        .should()
        .provision(
            new ProvisionUserCommand(
                "trainer", "trainer@pokedex.example", "Trainer@123", Role.USER));
    then(createLocalPokemon)
        .should()
        .create(
            new CreateLocalPokemonCommand(
                1,
                new ProprietaryValues(
                    Map.of("fr", "Bulbizarre"),
                    "Kanto",
                    "Grassland",
                    List.of("starter"),
                    "Seed starter")),
            "admin");
    then(createLocalPokemon)
        .should()
        .create(eq(new CreateLocalPokemonCommand(25, values(PIKACHU))), eq("admin"));
  }

  @Test
  void should_keepWhatAlreadyExists_when_runAgain() {
    given(provisionUser.provision(any())).willReturn(false);
    given(createLocalPokemon.create(any(), any()))
        .willThrow(new DuplicateLocalPokemonException(new PokemonId(1)));

    assertThat(seeder.seed()).isEqualTo(new DemoDataSeeder.Result(0, 2, 0, 0, 2, 0));
  }

  @Test
  void should_carryOnAndReportIt_when_pokeApiIsUnavailableForOnePokemon(CapturedOutput output) {
    // Given
    given(provisionUser.provision(any())).willReturn(true);
    given(createLocalPokemon.create(eq(new CreateLocalPokemonCommand(1, values(BULBASAUR))), any()))
        .willThrow(new CatalogUnavailableException("PokeAPI down", null));

    // When
    DemoDataSeeder.Result result = seeder.seed();

    // Then
    assertThat(result).isEqualTo(new DemoDataSeeder.Result(2, 0, 0, 1, 0, 1));
    assertThat(output).contains("#1").contains("next start");
  }

  @Test
  void should_carryOn_when_anotherAccountAlreadyUsesTheEmail() {
    given(provisionUser.provision(any()))
        .willThrow(new DuplicateUserException("email"))
        .willReturn(true);

    assertThat(seeder.seed()).isEqualTo(new DemoDataSeeder.Result(1, 0, 1, 2, 0, 0));
  }

  @Test
  void should_warnAboutPublicCredentialsWithoutPrintingThem_when_seeding(CapturedOutput output) {
    given(provisionUser.provision(any())).willReturn(true);

    seeder.seed();

    assertThat(output)
        .contains("admin, trainer")
        .contains("public deployment")
        .contains("accounts 2 created, 0 kept, 0 failed; local Pokémon 2 created, 0 kept, 0 failed")
        .doesNotContain("Admin@123")
        .doesNotContain("Trainer@123");
  }

  @Test
  void should_seed_when_runAsAnApplicationRunner() throws Exception {
    given(provisionUser.provision(any())).willReturn(true);

    seeder.run(null);

    then(createLocalPokemon)
        .should()
        .create(eq(new CreateLocalPokemonCommand(25, values(PIKACHU))), eq("admin"));
  }

  private static ProprietaryValues values(DemoData.LocalPokemon pokemon) {
    return new ProprietaryValues(
        pokemon.localizedNames(),
        pokemon.region(),
        pokemon.habitatNotes(),
        pokemon.internalTags(),
        pokemon.notes());
  }
}

package com.ballastlane.pokedex.adapter.in.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import com.ballastlane.pokedex.domain.user.EmailAddress;
import com.ballastlane.pokedex.domain.user.Role;
import com.ballastlane.pokedex.domain.user.Username;
import com.ballastlane.pokedex.domain.user.password.PasswordPolicy;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

/** The demo data that ships: what the README promises, and nothing the domain would refuse. */
class DemoDataFileTest {

  private final DemoData data = DemoDataReader.read(new ClassPathResource("seed/demo-data.json"));

  @Test
  void should_haveTheDocumentedAccounts() {
    assertThat(data.accounts())
        .containsExactly(
            new DemoData.Account("admin", "admin@pokedex.example", "Admin@123", Role.ADMIN),
            new DemoData.Account("trainer", "trainer@pokedex.example", "Trainer@123", Role.USER));
    assertThat(data.createdBy()).isEqualTo("admin");
  }

  @Test
  void should_haveAccountsTheDomainAccepts() {
    assertThat(data.accounts()).hasSize(2);
    for (DemoData.Account account : data.accounts()) {
      new Username(account.username());
      new EmailAddress(account.email());
      PasswordPolicy.standard().check(account.password());
    }
  }

  @Test
  void should_haveTheKantoStartersAndPikachu() {
    assertThat(data.localPokemon())
        .extracting(DemoData.LocalPokemon::pokeapiId)
        .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 25);
    assertThat(data.localPokemon())
        .filteredOn(pokemon -> pokemon.internalTags().contains("starter"))
        .extracting(DemoData.LocalPokemon::pokeapiId)
        .containsExactly(1, 4, 7);
  }

  @Test
  void should_haveProprietaryFieldsTheDomainAccepts() {
    assertThat(data.localPokemon())
        .hasSize(10)
        .allSatisfy(
            pokemon -> {
              ProprietaryData.of(
                  pokemon.localizedNames(),
                  pokemon.region(),
                  pokemon.habitatNotes(),
                  pokemon.internalTags(),
                  pokemon.notes());
              assertThat(pokemon.localizedNames()).containsKeys("ja", "fr", "de");
              assertThat(pokemon.region()).isEqualTo("Kanto");
            });
  }

  @Test
  void should_failNamingTheFile_when_itHasAnUnknownMember() {
    ByteArrayResource typo =
        new ByteArrayResource(
            "{\"createdBy\": \"admin\", \"acounts\": []}".getBytes(StandardCharsets.UTF_8),
            "typo seed");

    assertThatThrownBy(() -> DemoDataReader.read(typo))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("typo seed");
  }
}

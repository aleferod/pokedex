package com.ballastlane.pokedex.adapter.in.seed;

import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemon;
import com.ballastlane.pokedex.application.port.in.localpokemon.CreateLocalPokemonCommand;
import com.ballastlane.pokedex.application.port.in.localpokemon.ProprietaryValues;
import com.ballastlane.pokedex.application.port.in.user.ProvisionUser;
import com.ballastlane.pokedex.application.port.in.user.ProvisionUserCommand;
import com.ballastlane.pokedex.application.port.out.pokemon.CatalogUnavailableException;
import com.ballastlane.pokedex.domain.localpokemon.DuplicateLocalPokemonException;
import com.ballastlane.pokedex.domain.user.DuplicateUserException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Driving adapter: at startup, makes sure the demo accounts and local Pokémon exist, through the
 * same use cases as the API (password policy, BCrypt, domain rules, fresh PokeAPI data). Anything
 * that already exists is kept as it is, so restarts neither duplicate nor overwrite. A Pokémon that
 * PokeAPI cannot provide right now is reported and created on a later start. Spring Boot reports
 * readiness only after runners finish, so a healthy service already has its demo data.
 */
public class DemoDataSeeder implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

  /** What a run did. */
  public record Result(
      int accountsCreated,
      int accountsKept,
      int accountsFailed,
      int pokemonCreated,
      int pokemonKept,
      int pokemonFailed) {}

  private final DemoData data;
  private final ProvisionUser provisionUser;
  private final CreateLocalPokemon createLocalPokemon;

  public DemoDataSeeder(
      DemoData data, ProvisionUser provisionUser, CreateLocalPokemon createLocalPokemon) {
    this.data = data;
    this.provisionUser = provisionUser;
    this.createLocalPokemon = createLocalPokemon;
  }

  @Override
  public void run(ApplicationArguments args) {
    seed();
  }

  public Result seed() {
    LOG.warn(
        "Demo data is enabled: accounts {} have publicly documented passwords. Never enable"
            + " app.seed.enabled (SEED_DEMO_DATA) on a public deployment.",
        data.accounts().stream().map(DemoData.Account::username).collect(Collectors.joining(", ")));
    int accountsCreated = 0;
    int accountsKept = 0;
    int accountsFailed = 0;
    for (DemoData.Account account : data.accounts()) {
      try {
        if (provisionUser.provision(
            new ProvisionUserCommand(
                account.username(), account.email(), account.password(), account.role()))) {
          accountsCreated++;
        } else {
          accountsKept++;
        }
      } catch (DuplicateUserException emailTaken) {
        accountsFailed++;
        LOG.warn(
            "Demo account {} not created: another account already uses {}",
            account.username(),
            account.email());
      }
    }
    int pokemonCreated = 0;
    int pokemonKept = 0;
    int pokemonFailed = 0;
    for (DemoData.LocalPokemon pokemon : data.localPokemon()) {
      try {
        createLocalPokemon.create(
            new CreateLocalPokemonCommand(pokemon.pokeapiId(), values(pokemon)), data.createdBy());
        pokemonCreated++;
      } catch (DuplicateLocalPokemonException alreadyStored) {
        pokemonKept++;
      } catch (CatalogUnavailableException unavailable) {
        pokemonFailed++;
        LOG.warn(
            "Demo local Pokémon #{} not created: the Pokémon catalog is unavailable; it will be"
                + " tried again on the next start",
            pokemon.pokeapiId());
      }
    }
    Result result =
        new Result(
            accountsCreated,
            accountsKept,
            accountsFailed,
            pokemonCreated,
            pokemonKept,
            pokemonFailed);
    LOG.info(
        "Demo data: accounts {} created, {} kept, {} failed; local Pokémon {} created, {} kept,"
            + " {} failed",
        accountsCreated,
        accountsKept,
        accountsFailed,
        pokemonCreated,
        pokemonKept,
        pokemonFailed);
    return result;
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

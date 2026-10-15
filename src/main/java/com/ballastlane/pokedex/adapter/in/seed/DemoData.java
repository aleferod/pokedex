package com.ballastlane.pokedex.adapter.in.seed;

import com.ballastlane.pokedex.domain.user.Role;
import java.util.List;
import java.util.Map;

/**
 * The demo data file ({@code seed/demo-data.json}): who seeded records are attributed to, the demo
 * accounts and the local Pokémon with their proprietary fields, in the API's own shapes.
 */
public record DemoData(String createdBy, List<Account> accounts, List<LocalPokemon> localPokemon) {

  public DemoData {
    accounts = accounts == null ? List.of() : List.copyOf(accounts);
    localPokemon = localPokemon == null ? List.of() : List.copyOf(localPokemon);
  }

  /** A demo account. Its password is public on purpose (README) and never printed or logged. */
  public record Account(String username, String email, String password, Role role) {

    @Override
    public String toString() {
      return "Account[username="
          + username
          + ", email="
          + email
          + ", password=***, role="
          + role
          + "]";
    }
  }

  /** A local Pokémon to create: its PokeAPI number and proprietary fields. */
  public record LocalPokemon(
      int pokeapiId,
      Map<String, String> localizedNames,
      String region,
      String habitatNotes,
      List<String> internalTags,
      String notes) {}
}

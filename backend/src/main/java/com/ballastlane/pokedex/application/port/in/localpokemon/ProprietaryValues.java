package com.ballastlane.pokedex.application.port.in.localpokemon;

import com.ballastlane.pokedex.domain.localpokemon.ProprietaryData;
import java.util.List;
import java.util.Map;

/** Raw proprietary values from a client, validated by the domain when converted. */
public record ProprietaryValues(
    Map<String, String> localizedNames,
    String region,
    String habitatNotes,
    List<String> internalTags,
    String notes) {

  /**
   * @throws com.ballastlane.pokedex.domain.shared.InvalidFieldsException listing every rule the
   *     values break
   */
  public ProprietaryData toProprietaryData() {
    return ProprietaryData.of(localizedNames, region, habitatNotes, internalTags, notes);
  }
}

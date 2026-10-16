package com.ballastlane.pokedex.domain.localpokemon;

import java.time.Instant;
import java.util.Objects;

/** Who created a local record, and when it was created, last changed and last synchronized. */
public record AuditInfo(Instant createdAt, Instant updatedAt, Instant syncedAt, String createdBy) {

  public AuditInfo {
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    Objects.requireNonNull(syncedAt, "syncedAt");
    Objects.requireNonNull(createdBy, "createdBy");
  }
}

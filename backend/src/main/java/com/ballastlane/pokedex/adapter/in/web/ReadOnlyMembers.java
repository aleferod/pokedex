package com.ballastlane.pokedex.adapter.in.web;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Optional;

/** Reads {@link ReadOnlyFields}: is a member a request does not accept read-only, and why. */
public final class ReadOnlyMembers {

  private ReadOnlyMembers() {}

  /**
   * The read-only reason for {@code member} when {@code requestType} is annotated with {@link
   * ReadOnlyFields}, the representation has that member and the request does not; else empty.
   */
  public static Optional<String> reason(Class<?> requestType, String member) {
    if (requestType == null || member == null) {
      return Optional.empty();
    }
    ReadOnlyFields readOnly = requestType.getAnnotation(ReadOnlyFields.class);
    if (readOnly == null
        || !hasComponent(readOnly.value(), member)
        || hasComponent(requestType, member)) {
      return Optional.empty();
    }
    return Optional.of(readOnly.reason());
  }

  private static boolean hasComponent(Class<?> type, String name) {
    RecordComponent[] components = type.getRecordComponents();
    return components != null
        && Arrays.stream(components).anyMatch(component -> component.getName().equals(name));
  }
}

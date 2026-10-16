package com.ballastlane.pokedex.domain.localpokemon;

import java.util.Objects;

/**
 * One member of a JSON Merge Patch (RFC 7396), as a closed set of three cases: absent keeps the
 * current value, {@code null} clears it, and a value replaces it.
 */
public sealed interface FieldPatch<T> permits FieldPatch.Keep, FieldPatch.Clear, FieldPatch.Set {

  static <T> FieldPatch<T> keep() {
    return new Keep<>();
  }

  static <T> FieldPatch<T> clear() {
    return new Clear<>();
  }

  static <T> FieldPatch<T> set(T value) {
    return new Set<>(value);
  }

  /** The value after applying this patch to {@code current}. */
  T resolve(T current);

  /** The member was absent: keep the current value. */
  record Keep<T>() implements FieldPatch<T> {
    @Override
    public T resolve(T current) {
      return current;
    }
  }

  /** The member was {@code null}: remove the value. */
  record Clear<T>() implements FieldPatch<T> {
    @Override
    public T resolve(T current) {
      return null;
    }
  }

  /** The member had a value: replace the current one. */
  record Set<T>(T value) implements FieldPatch<T> {
    public Set {
      Objects.requireNonNull(value, "value (use clear() to remove a value)");
    }

    @Override
    public T resolve(T current) {
      return value;
    }
  }
}

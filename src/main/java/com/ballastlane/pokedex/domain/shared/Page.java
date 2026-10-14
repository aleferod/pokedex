package com.ballastlane.pokedex.domain.shared;

import java.util.List;

/** One page of a listing plus the totals needed to navigate it. Content is an immutable copy. */
public record Page<T>(List<T> content, int page, int size, long totalElements) {

  public Page {
    content = List.copyOf(content);
    if (totalElements < 0) {
      throw new IllegalArgumentException("totalElements must not be negative");
    }
  }

  public static <T> Page<T> of(List<T> content, PageRequest request, long totalElements) {
    return new Page<>(content, request.page(), request.size(), totalElements);
  }

  /** Number of pages needed for {@code totalElements}, rounded up. */
  public int totalPages() {
    return (int) ((totalElements + size - 1) / size);
  }
}

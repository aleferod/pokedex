package com.ballastlane.pokedex.adapter.in.web;

import com.ballastlane.pokedex.domain.shared.Page;
import java.util.List;
import java.util.function.Function;

/** The API's paginated envelope: {@code { content[], page, size, totalElements, totalPages }}. */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> toResponse) {
    return new PageResponse<>(
        page.content().stream().map(toResponse).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }
}

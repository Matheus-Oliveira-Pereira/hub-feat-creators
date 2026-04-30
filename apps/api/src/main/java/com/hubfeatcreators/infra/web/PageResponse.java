package com.hubfeatcreators.infra.web;

import java.util.List;

public record PageResponse<T>(List<T> data, Pagination pagination) {

  public record Pagination(String cursor, boolean hasMore, int limit) {}

  public static <T> PageResponse<T> of(List<T> data, String nextCursor, int limit) {
    return new PageResponse<>(data, new Pagination(nextCursor, nextCursor != null, limit));
  }
}

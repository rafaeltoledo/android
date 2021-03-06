package com.felipecsl.elifut.services;

import retrofit.Response;
import rx.functions.Func1;

/** Maps a {@link retrofit.Response} into its body of type {@link T} */
public final class ResponseBodyMapper<T> implements Func1<Response<T>, T> {
  private static final ResponseBodyMapper INSTANCE = new ResponseBodyMapper();

  private ResponseBodyMapper() {
  }

  public static <T> ResponseBodyMapper<T> instance() {
    //noinspection unchecked
    return INSTANCE;
  }

  @Override public T call(Response<T> response) {
    return response.body();
  }
}

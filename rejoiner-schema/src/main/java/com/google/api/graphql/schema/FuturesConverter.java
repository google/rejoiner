// Copyright 2017 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.graphql.schema;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

/** Converts an {@see ApiFuture} to a {@see ListenableFuture}. */
public final class FuturesConverter {
  private FuturesConverter() {}

  /** Converts an {@see ApiFuture} to a {@see ListenableFuture}. */
  public static <T> ListenableFuture<T> apiFutureToListenableFuture(final ApiFuture<T> apiFuture) {
    SettableFuture<T> settableFuture = SettableFuture.create();
    ApiFutures.addCallback(
        apiFuture,
        new ApiFutureCallback<T>() {
          @Override
          public void onFailure(Throwable t) {
            settableFuture.setException(t);
          }

          @Override
          public void onSuccess(T result) {
            settableFuture.set(result);
          }
        });
    return settableFuture;
  }

  public static Instrumentation apiFutureInstrumentation() {
    return new SimpleInstrumentation() {
      @Override
      public DataFetcher<?> instrumentDataFetcher(
          DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters) {
        return (DataFetcher<Object>)
            dataFetchingEnvironment -> {
              Object data = dataFetcher.get(dataFetchingEnvironment);
              if (data instanceof ApiFuture) {
                return FutureConverter.toCompletableFuture(
                    apiFutureToListenableFuture((ApiFuture<?>) data));
              }
              return data;
            };
      }
    };
  }
}

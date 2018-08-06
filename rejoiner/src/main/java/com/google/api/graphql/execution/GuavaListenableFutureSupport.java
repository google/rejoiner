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

package com.google.api.graphql.execution;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Adds support for ListenableFuture return values. */
public final class GuavaListenableFutureSupport {
  private GuavaListenableFutureSupport() {}

  /**
   * Converts a {@link ListenableFuture} to a Java8 {@link java.util.concurrent.CompletableFuture}.
   *
   * <p>{@see CompletableFuture} is supported by the provided {@link
   * graphql.execution.AsyncExecutionStrategy}.
   *
   * <p>Note: This should be installed before any other instrumentation.
   */
  public static Instrumentation listenableFutureInstrumentation() {
    return listenableFutureInstrumentation(MoreExecutors.directExecutor());
  }

  /**
   * Converts a {@link ListenableFuture} to a Java8 {@link java.util.concurrent.CompletableFuture}.
   *
   * <p>{@see CompletableFuture} is supported by the provided {@link
   * graphql.execution.AsyncExecutionStrategy}.
   *
   * <p>Note: This should be installed before any other instrumentation.
   */
  public static Instrumentation listenableFutureInstrumentation(Executor executor) {
    return new SimpleInstrumentation() {
      @Override
      public DataFetcher<?> instrumentDataFetcher(
          DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters) {
        return (DataFetcher<Object>)
            dataFetchingEnvironment -> {
              Object data = dataFetcher.get(dataFetchingEnvironment);
              if (data instanceof ListenableFuture) {
                ListenableFuture<Object> listenableFuture = (ListenableFuture<Object>) data;
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                Futures.addCallback(
                    listenableFuture,
                    new FutureCallback<Object>() {
                      @Override
                      public void onSuccess(Object result) {
                        completableFuture.complete(result);
                      }

                      @Override
                      public void onFailure(Throwable t) {
                        completableFuture.completeExceptionally(t);
                      }
                    },
                    executor);
                return completableFuture;
              }
              return data;
            };
      }
    };
  }

}

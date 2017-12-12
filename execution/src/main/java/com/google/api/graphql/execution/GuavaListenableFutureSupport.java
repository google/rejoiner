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

import com.google.common.util.concurrent.ListenableFuture;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.NoOpInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

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
    return new NoOpInstrumentation() {
      @Override
      public DataFetcher<?> instrumentDataFetcher(
          DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters) {
        return (DataFetcher<Object>)
            dataFetchingEnvironment -> {
              Object data = dataFetcher.get(dataFetchingEnvironment);
              if (data instanceof ListenableFuture) {
                return FutureConverter.<Object>toCompletableFuture((ListenableFuture<Object>) data);
              }
              return data;
            };
      }
    };
  }
}

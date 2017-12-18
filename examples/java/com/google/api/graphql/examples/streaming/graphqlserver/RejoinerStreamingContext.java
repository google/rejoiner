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

package com.google.api.graphql.examples.streaming.graphqlserver;

import com.google.auto.value.AutoValue;
import io.grpc.examples.graphql.GraphQlResponse;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: move this to a shared library
@AutoValue
abstract class RejoinerStreamingContext {

  abstract StreamObserver<GraphQlResponse> responseStreamObserver();

  abstract CountDownLatch countDownLatch();

  abstract AtomicInteger atomicInteger();

  public void startStream() {
    atomicInteger().addAndGet(1);
  }

  public void completeStream() {
    if (atomicInteger().decrementAndGet() <= 0) {
      countDownLatch().countDown();
    }
  }

  public void awaitStreams() throws InterruptedException {
    countDownLatch().await();
  }

  public static RejoinerStreamingContext create(StreamObserver<GraphQlResponse> stream) {
    return new AutoValue_RejoinerStreamingContext(
        stream, new CountDownLatch(1), new AtomicInteger(0));
  }
}

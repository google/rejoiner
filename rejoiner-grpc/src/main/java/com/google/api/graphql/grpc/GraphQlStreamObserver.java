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

package com.google.api.graphql.grpc;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class GraphQlStreamObserver<T extends Message, R extends Message>
    implements StreamObserver<T> {

  private final RejoinerStreamingContext rejoinerStreamingContext;
  private final DataFetchingEnvironment dataFetchingEnvironment;
  private final AtomicInteger pathIndex = new AtomicInteger();

  public GraphQlStreamObserver(DataFetchingEnvironment dataFetchingEnvironment) {
    this.dataFetchingEnvironment = dataFetchingEnvironment;
    rejoinerStreamingContext = dataFetchingEnvironment.getContext();
    rejoinerStreamingContext.startStream();
  }

  @Override
  public void onNext(T value) {
    List<Value> path =
        dataFetchingEnvironment
            .getExecutionStepInfo()
            .getPath()
            .toList()
            .stream()
            .map(
                p ->
                    p instanceof Number
                        ? Value.newBuilder()
                            .setNumberValue(Double.parseDouble(p.toString()))
                            .build()
                        : Value.newBuilder().setStringValue(p.toString()).build())
            .collect(ImmutableList.toImmutableList());

    ListValue pathListVale =
        ListValue.newBuilder()
            .addAllValues(path)
            .addValues(Value.newBuilder().setNumberValue(pathIndex.incrementAndGet()))
            .build();

    R graphQlResponse = getData(value, pathListVale);

    rejoinerStreamingContext.responseStreamObserver().onNext(graphQlResponse);

    try {
      System.out.println(
          "Streaming response as Json: " + JsonFormat.printer().print(graphQlResponse));
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract R getData(T value, ListValue path);

  @Override
  public void onError(Throwable t) {
    rejoinerStreamingContext.responseStreamObserver().onError(t);
  }

  @Override
  public void onCompleted() {
    rejoinerStreamingContext.completeStream();
  }
}

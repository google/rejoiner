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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.examples.graphql.GraphQlResponse;
import io.grpc.examples.graphql.QueryType;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: make this part of the framework
public abstract class GraphQlStreamObserver<T> implements StreamObserver<T> {

  private final RejoinerStreamingContext rejoinerStreamingContext;
  private final DataFetchingEnvironment dataFetchingEnvironment;
  private final AtomicInteger pathIndex = new AtomicInteger();

  GraphQlStreamObserver(DataFetchingEnvironment dataFetchingEnvironment) {
    this.dataFetchingEnvironment = dataFetchingEnvironment;
    rejoinerStreamingContext = dataFetchingEnvironment.getContext();
    rejoinerStreamingContext.startStream();
  }

  @Override
  public void onNext(T value) {
    List<Value> path =
        dataFetchingEnvironment
            .getFieldTypeInfo()
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
    GraphQlResponse graphQlResponse =
        GraphQlResponse.newBuilder()
            .setPath(
                ListValue.newBuilder()
                    .addAllValues(path)
                    .addValues(Value.newBuilder().setNumberValue(pathIndex.incrementAndGet())))
            .setData(getData(value))
            .build();

    rejoinerStreamingContext.responseStreamObserver().onNext(graphQlResponse);

    try {
      System.out.println(
          "Streaming response as Json: " + JsonFormat.printer().print(graphQlResponse));
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract QueryType getData(T value);

  @Override
  public void onError(Throwable t) {
    rejoinerStreamingContext.responseStreamObserver().onError(t);
  }

  @Override
  public void onCompleted() {
    rejoinerStreamingContext.completeStream();
  }
}

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

import com.google.api.graphql.grpc.GraphQlStreamObserver;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.protobuf.ListValue;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.examples.graphql.GraphQlResponse;
import io.grpc.examples.graphql.QueryType;
import io.grpc.examples.streaming.HelloReply;
import io.grpc.examples.streaming.HelloRequest;
import io.grpc.examples.streaming.StreamingGreeterGrpc;

/** A GraphQL {@link SchemaModule} backed by a streaming gRPC service. */
final class HelloWorldSchemaModule extends SchemaModule {

  @Query("sayHello")
  HelloReply sayHello(
      HelloRequest request,
      StreamingGreeterGrpc.StreamingGreeterStub client,
      DataFetchingEnvironment dataFetchingEnvironment) {
    client.sayHelloStreaming(
        request,
        new GraphQlStreamObserver<HelloReply, GraphQlResponse>(dataFetchingEnvironment) {
          @Override
          protected GraphQlResponse getData(HelloReply value, ListValue path) {
            // TODO: how can this be improved?
            QueryType data =
                QueryType.newBuilder()
                    .setHelloReply(
                        io.grpc.examples.graphql.HelloReply.newBuilder()
                            .setMessage(value.getMessage())
                            .build())
                    .build();

            return GraphQlResponse.newBuilder().setPath(path).setData(data).build();
          }
        });

    return null;
  }
}

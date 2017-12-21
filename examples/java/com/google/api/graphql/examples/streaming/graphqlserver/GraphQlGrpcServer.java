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

import com.google.api.graphql.execution.GuavaListenableFutureSupport;
import com.google.api.graphql.grpc.QueryResponseToProto;
import com.google.api.graphql.rejoiner.Schema;
import com.google.api.graphql.rejoiner.SchemaProviderModule;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLSchema;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.examples.graphql.GraphQlRequest;
import io.grpc.examples.graphql.GraphQlResponse;
import io.grpc.examples.graphql.GraphQlServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphQlGrpcServer {
  private static final Logger logger = Logger.getLogger(GraphQlGrpcServer.class.getName());

  private static final GraphQLSchema SCHEMA =
      Guice.createInjector(
              new SchemaProviderModule(),
              new HelloWorldClientModule(),
              new HelloWorldSchemaModule())
          .getInstance(Key.get(GraphQLSchema.class, Schema.class));

  private static final Instrumentation instrumentation =
      new ChainedInstrumentation(
          java.util.Arrays.asList(GuavaListenableFutureSupport.listenableFutureInstrumentation()));

  private static final GraphQL GRAPHQL =
      GraphQL.newGraphQL(SCHEMA).instrumentation(instrumentation).build();
  private Server server;

  private void start() throws IOException {
    /* The port on which the server should run */
    int port = 8888;
    server = ServerBuilder.forPort(port).addService(new GraphQlServiceImpl()).build().start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                GraphQlGrpcServer.this.stop();
                System.err.println("*** server shut down");
              }
            });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /** Await termination on the main thread since the grpc library uses daemon threads. */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /** Main launches the server from the command line. */
  public static void main(String[] args) throws IOException, InterruptedException {
    final GraphQlGrpcServer server = new GraphQlGrpcServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class GraphQlServiceImpl extends GraphQlServiceGrpc.GraphQlServiceImplBase {
    @Override
    public void execute(GraphQlRequest request, StreamObserver<GraphQlResponse> responseObserver) {
      // TODO: get variables
      Map<String, Object> variables = ImmutableMap.of();

      RejoinerStreamingContext context = RejoinerStreamingContext.create(responseObserver);

      ExecutionInput executionInput =
          ExecutionInput.newExecutionInput()
              .query(request.getQuery())
              .variables(variables)
              .context(context)
              .build();
      ExecutionResult executionResult = GRAPHQL.execute(executionInput);

      GraphQlResponse graphQlResponse =
          QueryResponseToProto.buildMessage(
              GraphQlResponse.getDefaultInstance(), executionResult.toSpecification());
      try {
        logger.info("Response in Json format: " + JsonFormat.printer().print(graphQlResponse));
      } catch (InvalidProtocolBufferException e) {
        logger.log(Level.WARNING, "Response proto is invalid", e);
      }
      responseObserver.onNext(graphQlResponse);

      try {
        logger.info("Waiting for streams");
        context.awaitStreams();
        logger.info("Done waiting for streams");
      } catch (InterruptedException e) {
        responseObserver.onError(e);
      } finally {
        responseObserver.onCompleted();
      }
    }
  }
}

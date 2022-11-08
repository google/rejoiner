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

package com.google.api.graphql.rejoiner;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.graphql.execution.GuavaListenableFutureSupport;
import com.google.api.graphql.rejoiner.Greetings.ExtraProto;
import com.google.api.graphql.rejoiner.Greetings.GreetingsRequest;
import com.google.api.graphql.rejoiner.Greetings.GreetingsResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.protobuf.ByteString;
import graphql.ErrorType;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import io.grpc.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for several aspects of Rejoiner. */
@RunWith(JUnit4.class)
public final class RejoinerIntegrationTest {

  static class SampleGraphQLException extends RuntimeException implements GraphQLError {
    @Override
    public String getMessage() {
      return "Test GraphQLError";
    }

    @Override
    public List<SourceLocation> getLocations() {
      return null;
    }

    @Override
    public ErrorType getErrorType() {
      return ErrorType.DataFetchingException;
    }

    @Override
    public Map<String, Object> getExtensions() {
      HashMap<String, Object> extensions = Maps.newHashMap();
      extensions.put("error", "message");
      return extensions;
    }
  }

  static class GreetingsSchemaModule extends SchemaModule {

    GreetingsSchemaModule() {
      super(SchemaOptions.builder().useProtoScalarTypes(true).build());
    }

    @Query("proto1")
    ListenableFuture<TestProto.Proto1> proto1() {
      return Futures.immediateFuture(
          TestProto.Proto1.newBuilder()
              .setCamelCaseName(101)
              .setId("id")
              .setIntField(1)
              .setNameField("name")
              .setTestInnerProto(TestProto.Proto1.InnerProto.newBuilder().setFoo("foooo"))
              .putAllMapField(
                  ImmutableMap.of(
                      "a", "1",
                      "b", "2",
                      "c", "3"))
              .setBytesField(ByteString.copyFromUtf8("b-y-t-e-s"))
              .build());
    }

    @Query("greetingXL")
    ListenableFuture<GreetingsResponse> greetingXl(
        GreetingsRequest req, DataFetchingEnvironment env) {
      return Futures.immediateFuture(GreetingsResponse.newBuilder().setId(req.getId()).build());
    }

    @Query("listOfStuff")
    ListenableFuture<ImmutableList<ExtraProto>> listOfStuff() {
      return Futures.immediateFuture(
          ImmutableList.of(ExtraProto.newBuilder().setSomeValue("1").build()));
    }

    @Query("listOfStuffSync")
    ImmutableList<ExtraProto> listOfStuffSync() {
      return ImmutableList.of(ExtraProto.newBuilder().setSomeValue("1").build());
    }

    @Query("greeting")
    ListenableFuture<GreetingsResponse> greetings(GreetingsRequest request) {
      return Futures.immediateFuture(GreetingsResponse.newBuilder().setId(request.getId()).build());
    }

    @Query("getAccountWithLanguages")
    Greetings.AccountValue accounts(@Arg("language") Greetings.Languages languages) {
      return Greetings.AccountValue.newBuilder().setAnEnum(languages).build();
    }

    @SchemaModification(addField = "extraField", onType = GreetingsResponse.class)
    ListenableFuture<ExtraProto> greetingsResponseToExtraProto(
        ExtraProto request, GreetingsResponse source) {
      return Futures.immediateFuture(request.toBuilder().setSomeValue(source.getId()).build());
    }

    @Query("greetingWithException")
    ListenableFuture<GreetingsResponse> greetingsWithException(GreetingsRequest request) {
      throw Status.UNIMPLEMENTED.withDescription("message from service").asRuntimeException();
    }

    @Query("greetingWithGraphQLError")
    ListenableFuture<GreetingsResponse> greetingsWithGraphqlError(GreetingsRequest request) {
      throw new SampleGraphQLException();
    }
  }

  static class GreetingsAddonSchemaModule extends SchemaModule {
    @SchemaModification
    TypeModification newGreeting =
        Type.find(GreetingsResponse.getDescriptor()).removeField("greeting");
  }

  private final GraphQLSchema schema =
      Guice.createInjector(
              new SchemaProviderModule(),
              new GreetingsSchemaModule(),
              new GreetingsAddonSchemaModule())
          .getInstance(Key.get(GraphQLSchema.class, Schema.class));

  @Test
  public void schemaShouldHaveNQueries() {
    assertThat(schema.getQueryType().getFieldDefinitions()).hasSize(8);
  }

  @Test
  public void schemaShouldList() {
    GraphQLOutputType listOfStuff =
        schema.getQueryType().getFieldDefinition("listOfStuff").getType();
    assertThat(listOfStuff).isInstanceOf(GraphQLList.class);
    assertThat(((GraphQLList) listOfStuff).getWrappedType()).isInstanceOf(GraphQLNonNull.class);
  }

  @Test
  public void schemaShouldListSync() {
    GraphQLOutputType listOfStuff =
        schema.getQueryType().getFieldDefinition("listOfStuffSync").getType();
    assertThat(listOfStuff).isInstanceOf(GraphQLList.class);
    assertThat(((GraphQLList) listOfStuff).getWrappedType()).isInstanceOf(GraphQLNonNull.class);
  }

  @Test
  public void schemaShouldGetAccountWithEnumArgs() {
    GraphQLFieldDefinition getAccount =
        schema.getQueryType().getFieldDefinition("getAccountWithLanguages");
    assertThat(getAccount.getArgument("language").getType()).isInstanceOf(GraphQLEnumType.class);
    assertThat(getAccount.getType()).isInstanceOf(GraphQLObjectType.class);
  }

  @Test
  public void schemaShouldHaveNoMutations() {
    assertThat(schema.getMutationType()).isNull();
  }

  @Test
  public void schemaModificationsShouldBeApplied() {
    GraphQLObjectType obj =
        (GraphQLObjectType)
            schema.getType(ProtoToGql.getReferenceName(GreetingsResponse.getDescriptor()));
    assertThat(obj.getFieldDefinitions()).hasSize(2);
    assertThat(obj.getFieldDefinition("id")).isNotNull();
    assertThat(obj.getFieldDefinition("extraField")).isNotNull();
    assertThat(obj.getFieldDefinition("greeting")).isNull();
  }

  @Test
  public void executionQueryWithEnumArgs() {
    GraphQL graphQL = GraphQL.newGraphQL(schema).build();
    ExecutionInput executionInput =
        ExecutionInput.newExecutionInput()
            .query("query { getAccountWithLanguages(language: EO) { anEnum } }")
            .build();
    ExecutionResult executionResult = graphQL.execute(executionInput);
    assertThat(executionResult.getErrors()).isEmpty();
  }

  @Test
  public void executionQueryWithMapResponse() {
    GraphQL graphQL =
        GraphQL.newGraphQL(schema)
            .instrumentation(GuavaListenableFutureSupport.listenableFutureInstrumentation())
            .build();
    ExecutionInput executionInput =
        ExecutionInput.newExecutionInput()
            .query("query { proto1 { mapField { key value } } }")
            .build();
    ExecutionResult executionResult = graphQL.execute(executionInput);
    assertThat(executionResult.getErrors()).isEmpty();
    assertThat(executionResult.toSpecification())
        .isEqualTo(
            ImmutableMap.of(
                "data",
                ImmutableMap.of(
                    "proto1",
                    ImmutableMap.of(
                        "mapField",
                        ImmutableList.of(
                            ImmutableMap.of("key", "a", "value", "1"),
                            ImmutableMap.of("key", "b", "value", "2"),
                            ImmutableMap.of("key", "c", "value", "3"))))));
  }

  @Test
  public void executionQueryWithAllFields() {
    GraphQL graphQL =
        GraphQL.newGraphQL(schema)
            .instrumentation(GuavaListenableFutureSupport.listenableFutureInstrumentation())
            .build();
    ExecutionInput executionInput =
        ExecutionInput.newExecutionInput()
            .query(
                "query { proto1 { "
                    + "mapField { key value } "
                    + "camelCaseName id intField RenamedField bytesField "
                    + "testInnerProto {foo} "
                    + "}}")
            .build();
    ExecutionResult executionResult = graphQL.execute(executionInput);
    assertThat(executionResult.getErrors()).isEmpty();
    assertThat(executionResult.toSpecification())
        .isEqualTo(
            ImmutableMap.of(
                "data",
                ImmutableMap.of(
                    "proto1",
                    ImmutableMap.builder()
                        .put(
                            "mapField",
                            ImmutableList.of(
                                ImmutableMap.of("key", "a", "value", "1"),
                                ImmutableMap.of("key", "b", "value", "2"),
                                ImmutableMap.of("key", "c", "value", "3")))
                        .put("camelCaseName", (long) 101)
                        .put("id", "id")
                        .put("intField", (long) 1)
                        .put("RenamedField", "name")
                        .put("testInnerProto", ImmutableMap.of("foo", "foooo"))
                        .put("bytesField", "b-y-t-e-s")
                        .build())));
  }

  @Test
  public void handlesRuntimeExceptionMessage() {
    GraphQL graphQL = GraphQL.newGraphQL(schema).build();

    ExecutionInput executionInput =
        ExecutionInput.newExecutionInput().query("query { greetingWithException { id } }").build();

    ExecutionResult executionResult = graphQL.execute(executionInput);

    assertThat(executionResult.getErrors()).hasSize(1);
    GraphQLError graphQLError = executionResult.getErrors().get(0);
    assertThat(graphQLError.getMessage())
        .isEqualTo(
            "Exception while fetching data (/greetingWithException) : UNIMPLEMENTED: message from service");
    assertThat(graphQLError.getPath()).hasSize(1);
    assertThat(graphQLError.getPath().get(0)).isEqualTo("greetingWithException");
  }

  @Test
  public void handlesGraphQLError() {
    GraphQL graphQL = GraphQL.newGraphQL(schema).build();

    ExecutionInput executionInput =
        ExecutionInput.newExecutionInput()
            .query("query { greetingWithGraphQLError { id } }")
            .build();

    ExecutionResult executionResult = graphQL.execute(executionInput);

    assertThat(executionResult.getErrors()).hasSize(1);
    GraphQLError graphQLError = executionResult.getErrors().get(0);
    assertThat(graphQLError.getMessage())
        .isEqualTo("Exception while fetching data (/greetingWithGraphQLError) : Test GraphQLError");
    assertThat(graphQLError.getExtensions()).containsEntry("error", "message");
    assertThat(graphQLError.getPath()).hasSize(1);
    assertThat(graphQLError.getPath().get(0)).isEqualTo("greetingWithGraphQLError");
  }
}

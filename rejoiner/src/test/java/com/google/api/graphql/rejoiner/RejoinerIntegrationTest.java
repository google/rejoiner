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

import com.google.api.graphql.rejoiner.Greetings.ExtraProto;
import com.google.api.graphql.rejoiner.Greetings.GreetingsRequest;
import com.google.api.graphql.rejoiner.Greetings.GreetingsResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Guice;
import com.google.inject.Key;
import graphql.ErrorType;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
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
  public void schemaShouldHaveOneQuery() {
    assertThat(schema.getQueryType().getFieldDefinitions()).hasSize(6);
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

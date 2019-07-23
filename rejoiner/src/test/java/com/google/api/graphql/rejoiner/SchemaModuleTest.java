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

import com.google.api.graphql.rejoiner.Greetings.GreetingsRequest;
import com.google.api.graphql.rejoiner.Greetings.GreetingsResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.CreationException;
import com.google.inject.TypeLiteral;
import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link SchemaModule}. */
@RunWith(JUnit4.class)
public final class SchemaModuleTest {

  private static final Key<Set<SchemaBundle>> KEY =
      Key.get(new TypeLiteral<Set<SchemaBundle>>() {}, Annotations.SchemaBundles.class);

  @Test
  public void schemaModuleShouldProvideEmpty() {
    Injector injector = Guice.createInjector(new SchemaModule() {});
    assertThat(injector.getInstance(KEY)).isNotNull();
  }

  @Test
  public void schemaModuleShouldProvideQueryFields() {
    Injector injector =
        Guice.createInjector(
            new SchemaModule() {
              @Query
              GraphQLFieldDefinition greeting =
                  GraphQLFieldDefinition.newFieldDefinition()
                      .name("greeting")
                      .type(Scalars.GraphQLString)
                      .build();
            });
    SchemaBundle schemaBundle = SchemaBundle.combine(injector.getInstance(KEY));
    assertThat(schemaBundle.queryFields()).hasSize(1);
  }

  @Test
  public void schemaModuleShouldProvideQueryAndMutationFields() {
    Injector injector =
        Guice.createInjector(
            new SchemaModule() {
              @Query
              GraphQLFieldDefinition greeting =
                  GraphQLFieldDefinition.newFieldDefinition()
                      .name("queryField")
                      .type(Scalars.GraphQLString)
                      .build();

              @Mutation
              GraphQLFieldDefinition mutationField =
                  GraphQLFieldDefinition.newFieldDefinition()
                      .name("mutationField")
                      .type(Scalars.GraphQLString)
                      .build();

              @Query("queryMethod")
              GreetingsResponse queryMethod(GreetingsRequest request) {
                return GreetingsResponse.newBuilder().setId(request.getId()).build();
              }

              @Mutation("mutationMethod")
              ListenableFuture<GreetingsResponse> mutationMethod(GreetingsRequest request) {
                return Futures.immediateFuture(
                    GreetingsResponse.newBuilder().setId(request.getId()).build());
              }
            });

    SchemaBundle schemaBundle = SchemaBundle.combine(injector.getInstance(KEY));
    assertThat(schemaBundle.queryFields()).hasSize(2);
    assertThat(schemaBundle.mutationFields()).hasSize(2);
    assertThat(schemaBundle.fileDescriptors()).hasSize(1);
    assertThat(schemaBundle.modifications()).isEmpty();
  }

  @Test
  public void schemaModuleShouldNamespaceQueriesAndMutations() {
    @Namespace("namespace")
    class NamespacedSchemaModule extends SchemaModule {
      @Query
      GraphQLFieldDefinition greeting =
          GraphQLFieldDefinition.newFieldDefinition()
              .name("queryField")
              .type(Scalars.GraphQLString)
              .build();

      @Mutation
      GraphQLFieldDefinition mutationField =
          GraphQLFieldDefinition.newFieldDefinition()
              .name("mutationField")
              .type(Scalars.GraphQLString)
              .build();

      @Query("queryMethod")
      GreetingsResponse queryMethod(GreetingsRequest request) {
        return GreetingsResponse.newBuilder().setId(request.getId()).build();
      }

      @Mutation("mutationMethod")
      ListenableFuture<GreetingsResponse> mutationMethod(GreetingsRequest request) {
        return Futures.immediateFuture(
            GreetingsResponse.newBuilder().setId(request.getId()).build());
      }
    }
    Injector injector = Guice.createInjector(new NamespacedSchemaModule());
    SchemaBundle schemaBundle = SchemaBundle.combine(injector.getInstance(KEY));
    assertThat(schemaBundle.queryFields()).hasSize(1);
    assertThat(schemaBundle.mutationFields()).hasSize(1);
    assertThat(schemaBundle.fileDescriptors()).hasSize(1);
    assertThat(schemaBundle.modifications()).isEmpty();
  }

  @Test
  public void schemaModuleShouldApplyArgs() {

    Injector injector =
        Guice.createInjector(
            new SchemaModule() {

              @Mutation("mutationMethodWithArgs")
              ListenableFuture<GreetingsResponse> mutationMethod(
                  GreetingsRequest request, @Arg("showDeleted") Boolean showDeleted) {

                return Futures.immediateFuture(
                    GreetingsResponse.newBuilder().setId(request.getId()).build());
              }
            });
    SchemaBundle schemaBundle = SchemaBundle.combine(injector.getInstance(KEY));
    assertThat(schemaBundle.mutationFields()).hasSize(1);

    List<GraphQLArgument> arguments =
        schemaBundle.mutationFields().iterator().next().getArguments();
    assertThat(arguments).hasSize(2);
    assertThat(
            arguments
                .stream()
                .map(argument -> argument.getName())
                .collect(ImmutableList.toImmutableList()))
        .containsExactly("input", "showDeleted");
  }

  @Test
  public void schemaShouldValidateWhenIncludingDataFetchingEnvironment() throws Exception {
    Injector injector =
        Guice.createInjector(
            new SchemaModule() {
              @Query("hello")
              ListenableFuture<GreetingsResponse> querySnippets(
                  GreetingsRequest request, DataFetchingEnvironment environment) {
                return Futures.immediateFuture(
                    GreetingsResponse.newBuilder().setId(request.getId()).build());
              }
            });
    validateSchema(injector);
  }

  @Test
  public void schemaShouldValidateWhenProvidingJustRequest() throws Exception {
    Injector injector =
        Guice.createInjector(
            new SchemaModule() {
              @Query("hello")
              ListenableFuture<GreetingsResponse> querySnippets(GreetingsRequest request) {
                return Futures.immediateFuture(
                    GreetingsResponse.newBuilder().setId(request.getId()).build());
              }
            });
    validateSchema(injector);
  }

  @Test
  public void schemaShouldValidateWhenInjectingParameterUsingGuice() throws Exception {
    Injector injector =
        Guice.createInjector(
            new SchemaModule() {
              @Query("hello")
              ListenableFuture<GreetingsResponse> querySnippets(GreetingsRequest request) {
                return Futures.immediateFuture(
                    GreetingsResponse.newBuilder().setId(request.getId()).build());
              }
            });
    validateSchema(injector);
  }

  private void validateSchema(Injector injector) throws Exception {
    SchemaBundle schemaBundle = SchemaBundle.combine(injector.getInstance(KEY));
    assertThat(schemaBundle.queryFields()).hasSize(1);
    assertThat(schemaBundle.mutationFields()).isEmpty();
    assertThat(schemaBundle.fileDescriptors()).hasSize(1);
    assertThat(schemaBundle.modifications()).isEmpty();
    Collection<GraphQLFieldDefinition> queryFields = schemaBundle.queryFields();
    GraphQLFieldDefinition hello = queryFields.iterator().next();
    assertThat(hello.getName()).isEqualTo("hello");
    assertThat(hello.getArguments()).hasSize(1);
    assertThat(hello.getArgument("input")).isNotNull();
    assertThat(hello.getArgument("input").getType().getName())
        .isEqualTo("Input_javatests_com_google_api_graphql_rejoiner_proto_GreetingsRequest");

    assertThat(hello.getType().getName())
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_GreetingsResponse");

    // TODO: migrate test to use GraphQLCodeRegistry
    //    Object result =
    //        hello
    //            .getDataFetcher()
    //            .get(
    //                DataFetchingEnvironmentBuilder.newDataFetchingEnvironment()
    //                    .executionContext(
    //                        ExecutionContextBuilder.newExecutionContextBuilder()
    //                            .executionId(ExecutionId.from("1"))
    //                            .build())
    //                    .arguments(ImmutableMap.of("input", ImmutableMap.of("id", "123")))
    //                    .build());
    //
    //    assertThat(result).isInstanceOf(ListenableFuture.class);
    //    assertThat(((ListenableFuture<?>) result).get())
    //        .isEqualTo(GreetingsResponse.newBuilder().setId("123").build());
  }

  @Test
  @Ignore("why shouldn't it fail?")
  public void schemaModuleShouldNotFailOnInjectorCreation() {
    Injector injector =
        Guice.createInjector(
            new SchemaModule() {
              @Query String greeting = "hi";
            });
  }

  @Test(expected = CreationException.class)
  public void schemaModuleShouldFailIfWrongTypeIsAnnotated() {
    Injector injector =
        Guice.createInjector(
            new SchemaModule() {
              @Query String greeting = "hi";
            });

    // TODO: replace with assertThrows(()->injector.getInstance(KEY), ProvisionException.class)
    // and remove schemaModuleShouldNotFailOnInjectorCreation
    injector.getInstance(KEY);
  }
}

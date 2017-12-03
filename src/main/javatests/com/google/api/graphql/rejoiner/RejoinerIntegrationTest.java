package com.google.api.graphql.rejoiner;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.graphql.rejoiner.Greetings.ExtraProto;
import com.google.api.graphql.rejoiner.Greetings.GreetingsRequest;
import com.google.api.graphql.rejoiner.Greetings.GreetingsResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Guice;
import com.google.inject.Key;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for several aspects of Rejoiner. */
@RunWith(JUnit4.class)
public final class RejoinerIntegrationTest {

  static class GreetingsSchemaModule extends SchemaModule {

    static final GqlInputConverter INPUT_CONVERTER =
        GqlInputConverter.newBuilder().add(GreetingsRequest.getDescriptor().getFile()).build();

    @Query("grettingXL")
    ListenableFuture<GreetingsResponse> greetingXl(
        GreetingsRequest req, DataFetchingEnvironment env) {
      return Futures.immediateFuture(GreetingsResponse.newBuilder().setId(req.getId()).build());
    }

    @Query("listOfStuff")
    ListenableFuture<ImmutableList<ExtraProto>> listOfStuff() {
      return Futures.immediateFuture(
          ImmutableList.of(ExtraProto.newBuilder().setSomeValue("1").build()));
    }

    @Query("greeting")
    ListenableFuture<GreetingsResponse> greetings(/*TODO: Fix this GreetingsRequest request*/) {
      return Futures.immediateFuture(GreetingsResponse.newBuilder().setId("10").build());
    }
    @SchemaModification(addField = "extraField", onType = GreetingsResponse.class)
    ListenableFuture<ExtraProto> greetingsResponseToExtraProto(
        ExtraProto request, GreetingsResponse source) {
      return Futures.immediateFuture(request.toBuilder().setSomeValue(source.getId()).build());
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
    assertThat(schema.getQueryType().getFieldDefinitions()).hasSize(3);
  }

  @Test
  public void schemaShouldList() {
    assertThat(schema.getQueryType().getFieldDefinition("listOfStuff").getType())
        .isInstanceOf(GraphQLList.class);
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
}

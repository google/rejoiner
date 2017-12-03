package com.google.api.graphql.rejoiner;

import static com.google.common.truth.Truth.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.api.graphql.rejoiner.SchemaProviderModule}. */
@RunWith(JUnit4.class)
public final class SchemaProviderModuleTest {

  @Test
  public void schemaModuleShouldProvideEmptySchema() {
    Injector injector =
        Guice.createInjector(new SchemaProviderModule(), new SchemaModule() {});
    assertThat(injector.getInstance(Key.get(GraphQLSchema.class, Schema.class))).isNotNull();
    assertThat(injector.getInstance(Key.get(GraphQLSchema.class, Schema.class)).getQueryType())
        .isNotNull();
  }

  @Test
  public void schemaModuleShouldProvideQueryType() {
    Injector injector =
        Guice.createInjector(
            new SchemaProviderModule(),
            new SchemaModule() {
              @Query
              GraphQLFieldDefinition greeting =
                  GraphQLFieldDefinition.newFieldDefinition()
                      .name("greeting")
                      .type(Scalars.GraphQLString)
                      .staticValue("hello world")
                      .build();
            });
    assertThat(
            injector
                .getInstance(Key.get(GraphQLSchema.class, Schema.class))
                .getQueryType()
                .getFieldDefinition("greeting"))
        .isNotNull();
  }

  @Test
  public void schemaModuleShouldModifyTypes() {
    Injector injector =
        Guice.createInjector(
            new SchemaProviderModule(),
            new SchemaModule() {
              @Query
              GraphQLFieldDefinition greeting =
                  GraphQLFieldDefinition.newFieldDefinition()
                      .name("greeting")
                      .type(Scalars.GraphQLString)
                      .staticValue("hello world")
                      .build();
            },
            new SchemaModule() {
              @SchemaModification
              TypeModification newGreeting = Type.find("QueryType").removeField("greeting");
            });
    assertThat(
            injector
                .getInstance(Key.get(GraphQLSchema.class, Schema.class))
                .getQueryType()
                .getFieldDefinitions())
        .hasSize(1);
    //TODO: this should be empty, currently type modifications only apply to types
    // annotated with ExtraTypes.
  }
}

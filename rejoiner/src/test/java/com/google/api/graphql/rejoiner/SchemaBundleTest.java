package com.google.api.graphql.rejoiner;

import static com.google.common.truth.Truth.assertThat;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.junit.Test;

public class SchemaBundleTest {
  @Test
  public void createSchemaUsingSchemaBundle() {
    final SchemaBundle.Builder schemaBuilder = AutoValue_SchemaBundle.builder();
    schemaBuilder
        .modificationsBuilder()
        .add(
            Type.find("inner")
                .addField(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name("bazinga")
                        .type(Scalars.GraphQLBigInteger)
                        .build()));
    schemaBuilder
        .queryFieldsBuilder()
        .add(
            GraphQLFieldDefinition.newFieldDefinition()
                .name("bazinga")
                .type(GraphQLObjectType.newObject().name("inner"))
                .build());
    final SchemaBundle schemaBundle = schemaBuilder.build();
    final GraphQLSchema schema = schemaBundle.toSchema();
    assertThat(schema.getQueryType().getFieldDefinitions()).hasSize(1);
    assertThat(schema.getQueryType().getFieldDefinitions().get(0).getName()).isEqualTo("bazinga");
  }
}

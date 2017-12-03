package com.google.api.graphql.rejoiner;

import static com.google.common.truth.Truth.assertThat;

import graphql.AssertException;
import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.api.graphql.rejoiner.Type}. */
@RunWith(JUnit4.class)
public final class TypeTest {

  private static final GraphQLObjectType OBJECT_TYPE =
      GraphQLObjectType.newObject()
          .name("project")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("name")
                  .staticValue("rejoiner")
                  .type(Scalars.GraphQLString)
                  .build())
          .build();

  @Test
  public void addFieldShouldAddField() throws Exception {
    TypeModification typeModification =
        Type.find("project")
            .addField(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("isTheBest")
                    .staticValue(true)
                    .type(Scalars.GraphQLBoolean)
                    .build());

    assertThat(typeModification.apply(OBJECT_TYPE).getFieldDefinition("isTheBest")).isNotNull();
  }

  @Test
  public void removeFieldShouldRemoveField() throws Exception {
    TypeModification typeModification = Type.find("project").removeField("name");
    assertThat(typeModification.apply(OBJECT_TYPE).getFieldDefinition("project")).isNull();
  }

  @Test
  public void replaceFieldShouldReplaceField() throws Exception {
    TypeModification typeModification =
        Type.find("project")
            .replaceField(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("name")
                    .staticValue("rejoinerv2")
                    .type(Scalars.GraphQLString)
                    .build());
    assertThat(
            typeModification
                .apply(OBJECT_TYPE)
                .getFieldDefinition("name")
                .getDataFetcher()
                .get(null))
        .isEqualTo("rejoinerv2");
  }

  @Test(expected = AssertException.class)
  public void addFieldShouldThrowErrorIfFieldExists() throws Exception {
    Type.find("project")
        .addField(
            GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .staticValue("rejoiner2")
                .type(Scalars.GraphQLString)
                .build())
        .apply(OBJECT_TYPE);
  }

  @Test
  public void removeFieldShouldIgnoreUnknownField() throws Exception {
    Type.find("project").removeField("unknown_field").apply(OBJECT_TYPE);
  }
}

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

import graphql.AssertException;
import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
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
                    .type(Scalars.GraphQLInt)
                    .build());
    assertThat(typeModification.apply(OBJECT_TYPE).getFieldDefinition("name").getType())
        .isEqualTo(Scalars.GraphQLInt);
  }

  @Test(expected = AssertException.class)
  public void addFieldShouldThrowErrorIfFieldExists() throws Exception {
    Type.find("project")
        .addField(
            GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(Scalars.GraphQLString)
                .build())
        .apply(OBJECT_TYPE);
  }

  @Test
  public void removeFieldShouldIgnoreUnknownField() throws Exception {
    Type.find("project").removeField("unknown_field").apply(OBJECT_TYPE);
  }
}

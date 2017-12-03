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

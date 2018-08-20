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

package com.google.api.graphql.grpc;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

/** Unit tests for {@link DynamicProtoBuilder}. */
@RunWith(JUnit4.class)
public final class DynamicProtoBuilderTest {

  @Test
  public void generatedProto() {
    assertThat(
            DynamicProtoBuilder.createProto(
                    GraphQLSchema.newSchema()
                        .query(
                            GraphQLObjectType.newObject()
                                .name("queryType")
                                .field(
                                    GraphQLFieldDefinition.newFieldDefinition()
                                        .name("string")
                                        .staticValue(new Object())
                                        .type(Scalars.GraphQLString))
                                .field(
                                    GraphQLFieldDefinition.newFieldDefinition()
                                        .name("boolean")
                                        .staticValue(new Object())
                                        .type(Scalars.GraphQLBoolean))
                                .field(
                                    GraphQLFieldDefinition.newFieldDefinition()
                                        .name("float")
                                        .staticValue(new Object())
                                        .type(Scalars.GraphQLFloat))
                                .field(
                                    GraphQLFieldDefinition.newFieldDefinition()
                                        .name("int")
                                        .staticValue(new Object())
                                        .type(Scalars.GraphQLInt))
                                .field(
                                    GraphQLFieldDefinition.newFieldDefinition()
                                        .name("long")
                                        .staticValue(new Object())
                                        .type(Scalars.GraphQLLong))
                                .field(
                                    GraphQLFieldDefinition.newFieldDefinition()
                                        .name("message")
                                        .staticValue(new Object())
                                        .type(
                                            GraphQLObjectType.newObject()
                                                .name("messageType")
                                                .field(
                                                    GraphQLFieldDefinition.newFieldDefinition()
                                                        .name("string1")
                                                        .staticValue(new Object())
                                                        .type(Scalars.GraphQLString))
                                                .field(
                                                    GraphQLFieldDefinition.newFieldDefinition()
                                                        .name("string2")
                                                        .staticValue(new Object())
                                                        .type(Scalars.GraphQLString)))))
                        .build(),
                    "{string boolean int long a:message{string1 string2} message{string1 string2}}")
                .toString())
        .isEqualTo("error");
  }
}

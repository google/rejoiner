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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import graphql.Scalars;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import java.util.ArrayList;
import java.util.stream.Collectors;

/** Creates a type script type def file for a {@link GraphQLSchema}. */
public final class SchemaToTypeScript {

  private static final String HEADER = "// tslint:disable:class-name \n\n";

  private static final ImmutableMap<String, String> TYPE_MAP =
      new ImmutableMap.Builder<String, String>()
          .put(Scalars.GraphQLBoolean.getName(), "boolean")
          .put(Scalars.GraphQLFloat.getName(), "number")
          .put(Scalars.GraphQLInt.getName(), "number")
          .put(Scalars.GraphQLLong.getName(), "number")
          .put(Scalars.GraphQLString.getName(), "string")
          .build();

  /** Returns a proto source file for the schema. */
  public static String toTs(GraphQLSchema schema) {
    ArrayList<String> messages = new ArrayList<>();
    for (GraphQLType type : SchemaToProto.getAllTypes(schema)) {
      if (type instanceof GraphQLEnumType) {
        messages.add(toEnum((GraphQLEnumType) type));
      } else if (type instanceof GraphQLObjectType) {
        messages.add(toMessage((GraphQLObjectType) type));
      }
    }
    return HEADER + Joiner.on("\n\n").join(messages);
  }

  private static String toEnum(GraphQLEnumType type) {
    String types =
        type.getValues()
            .stream()
            .map(value -> value.getName())
            .filter(name -> !name.equals("UNRECOGNIZED"))
            .collect(Collectors.joining(", \n  "));
    return String.format("export enum %s {\n  %s\n}", type.getName() + "Enum", types);
  }

  private static String toMessage(GraphQLObjectType type) {
    String fields =
        type.getFieldDefinitions()
            .stream()
            .filter(field -> !field.getName().equals("_"))
            .map(field -> toField(field))
            .collect(Collectors.joining("\n"));
    return String.format("export interface %s {\n%s\n}", type.getName(), fields);
  }

  private static String toField(GraphQLFieldDefinition field) {
    return String.format("  %s: %s;", field.getName(), toType(field.getType()));
  }

  private static String toType(GraphQLType type) {
    if (type instanceof GraphQLList) {
      return toType(((GraphQLList) type).getWrappedType()) + "[]";
    } else if (type instanceof GraphQLObjectType) {
      return type.getName();
    } else if (type instanceof GraphQLEnumType) {
      return type.getName() + "Enum";
    } else {
      return TYPE_MAP.get(type.getName());
    }
  }

  private SchemaToTypeScript() {}
}

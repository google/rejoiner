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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.DescriptorProtos;
import graphql.Scalars;
import graphql.analysis.QueryTraversal;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionContextBuilder;
import graphql.execution.ExecutionId;
import graphql.language.Field;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

public final class DynamicProtoBuilder {

  private static final Field ROOT_FIELD = new Field();

  public static DescriptorProtos.DescriptorProto createProto(GraphQLSchema schema, String query) {

    // TODO: handle enum
    ExecutionContext executionContext =
        ExecutionContextBuilder.newExecutionContextBuilder()
            .graphQLSchema(schema)
            .variables(ImmutableMap.of())
            .document(new Parser().parseDocument(query))
            .executionId(ExecutionId.from(""))
            .build();

    QueryTraversal queryTraversal =
        QueryTraversal.newQueryTraversal()
            .schema(executionContext.getGraphQLSchema())
            .document(executionContext.getDocument())
            .variables(executionContext.getVariables())
            .build();

    /** A map from the parent selector (message) to all it's fields. */
    ImmutableMultimap.Builder<Field, DescriptorProtos.FieldDescriptorProto> fieldMap =
        ImmutableMultimap.builder();

    /** A map from the parent selector to all it's sub messages. */
    ImmutableMultimap.Builder<Field, DescriptorProtos.DescriptorProto> messageMap =
        ImmutableMultimap.builder();

    // reducing is set of top level
    queryTraversal.reducePostOrder(
        (env, unused) -> {
          final Field parent =
              env.getParentEnvironment() == null
                  ? ROOT_FIELD
                  : env.getParentEnvironment().getField();
          // Create the field (and optionally nested message)
          DescriptorProtos.FieldDescriptorProto.Builder fieldDescriptorProto =
              DescriptorProtos.FieldDescriptorProto.newBuilder().setName(fieldAliasOrName(env));
          if (SCALAR_TYPE_MAP.containsKey(env.getFieldDefinition().getType())) {
            fieldDescriptorProto.setType(SCALAR_TYPE_MAP.get(env.getFieldDefinition().getType()));
          } else {
            String typeName = fieldAliasOrName(env) + "Type";
            fieldDescriptorProto.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE);
            fieldDescriptorProto.setTypeName(typeName);

            // add all fields and nested types to message
            DescriptorProtos.DescriptorProto.Builder messageBuilder =
                DescriptorProtos.DescriptorProto.newBuilder().setName(typeName);
            messageMap.build().get(env.getField()).forEach(messageBuilder::addNestedType);
            fieldMap.build().get(env.getField()).forEach(messageBuilder::addField);

            // add message to message map
            messageMap.put(parent, messageBuilder.build());
          }

          // Add the field to the parent or root
          fieldMap.put(parent, fieldDescriptorProto.build());
          return "";
        },
        "");
    DescriptorProtos.DescriptorProto.Builder graphqlResponse =
        DescriptorProtos.DescriptorProto.newBuilder().setName("GraphqlResponse");

    messageMap.build().get(ROOT_FIELD).forEach(graphqlResponse::addNestedType);
    fieldMap.build().get(ROOT_FIELD).forEach(graphqlResponse::addField);

    return graphqlResponse.build();
  }

  private static String fieldAliasOrName(QueryVisitorFieldEnvironment env) {
    return env.getField().getAlias() == null
        ? env.getFieldDefinition().getName()
        : env.getField().getAlias();
  }

  private static final ImmutableMap<GraphQLType, DescriptorProtos.FieldDescriptorProto.Type>
      SCALAR_TYPE_MAP =
          new ImmutableMap.Builder<GraphQLType, DescriptorProtos.FieldDescriptorProto.Type>()
              .put(Scalars.GraphQLBoolean, DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL)
              .put(Scalars.GraphQLFloat, DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT)
              .put(Scalars.GraphQLInt, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32)
              .put(Scalars.GraphQLLong, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64)
              .put(Scalars.GraphQLString, DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
              .build();
}

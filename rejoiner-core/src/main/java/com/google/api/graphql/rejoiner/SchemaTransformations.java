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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.SchemaTraverser;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Common GraphQL Schema transformations. */
public final class SchemaTransformations {

  private SchemaTransformations() {}

  /** Removes all named types that aren't reachable from additionalTypes. */
  public static Consumer<GraphQLSchema.Builder> removeUnreachableTypes() {
    return builder -> {
      GraphQLSchema inputSchema = builder.build();
      ImmutableList<GraphQLType> graphQLTypes =
          ImmutableList.copyOf(inputSchema.getAdditionalTypes());
      Set<String> reachableTypes = new HashSet<>();
      new SchemaTraverser()
          .depthFirst(
              new GraphQLTypeVisitorStub() {
                @Override
                public TraversalControl visitGraphQLInterfaceType(
                    GraphQLInterfaceType node, TraverserContext<GraphQLSchemaElement> context) {
                  reachableTypes.add(node.getName());
                  return super.visitGraphQLInterfaceType(node, context);
                }

                @Override
                public TraversalControl visitGraphQLEnumType(
                    GraphQLEnumType node, TraverserContext<GraphQLSchemaElement> context) {
                  reachableTypes.add(node.getName());
                  return super.visitGraphQLEnumType(node, context);
                }

                @Override
                public TraversalControl visitGraphQLInputObjectType(
                    GraphQLInputObjectType node, TraverserContext<GraphQLSchemaElement> context) {
                  reachableTypes.add(node.getName());
                  return super.visitGraphQLInputObjectType(node, context);
                }

                @Override
                public TraversalControl visitGraphQLObjectType(
                    GraphQLObjectType node, TraverserContext<GraphQLSchemaElement> context) {
                  reachableTypes.add(node.getName());
                  return super.visitGraphQLObjectType(node, context);
                }

                @Override
                public TraversalControl visitGraphQLTypeReference(
                    GraphQLTypeReference node, TraverserContext<GraphQLSchemaElement> context) {
                  reachableTypes.add(node.getName());
                  return super.visitGraphQLTypeReference(node, context);
                }

                @Override
                protected TraversalControl visitGraphQLType(
                    GraphQLSchemaElement node, TraverserContext<GraphQLSchemaElement> context) {
                  if (node instanceof GraphQLNamedType) {
                    reachableTypes.add(((GraphQLNamedType) node).getName());
                  }
                  return super.visitGraphQLType(node, context);
                }
              },
              inputSchema.isSupportingMutations()
                  ? ImmutableList.of(inputSchema.getQueryType(), inputSchema.getMutationType())
                  : ImmutableList.of(inputSchema.getQueryType()));

      builder.clearAdditionalTypes();
      builder.additionalTypes(
          graphQLTypes.stream()
              .filter(
                  type ->
                      !(type instanceof GraphQLNamedType)
                          || reachableTypes.contains(((GraphQLNamedType) type).getName()))
              .collect(ImmutableSet.toImmutableSet()));
    };
  }
}

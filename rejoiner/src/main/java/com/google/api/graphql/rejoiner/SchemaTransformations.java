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
                  reachableTypes.add(((GraphQLNamedType) node).getName());
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

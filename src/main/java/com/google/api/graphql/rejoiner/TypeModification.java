package com.google.api.graphql.rejoiner;

import com.google.common.base.Function;
import graphql.schema.GraphQLObjectType;

/** Modifies a GraphQL type when creating a Schema. */
public interface TypeModification extends Function<GraphQLObjectType, GraphQLObjectType> {

  /** The type to modify. */
  String getTypeName();
}

package com.google.api.graphql.rejoiner;

import com.google.auto.value.AutoValue;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;

/** A GraphQL field definition with it's data fetcher. */
@AutoValue
public abstract class FieldDefinition<T> {
  abstract String parentTypeName();

  abstract GraphQLFieldDefinition field();

  abstract DataFetcher<T> dataFetcher();

  static <T> FieldDefinition<T> create(
      String parentTypeName, GraphQLFieldDefinition field, DataFetcher<T> dataFetcher) {
    return new AutoValue_FieldDefinition<T>(parentTypeName, field, dataFetcher);
  }

  String fieldName() {
    return field().getName();
  }
}

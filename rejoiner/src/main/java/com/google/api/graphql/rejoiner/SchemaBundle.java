package com.google.api.graphql.rejoiner;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors;
import graphql.schema.GraphQLFieldDefinition;

import javax.inject.Provider;
import java.util.Collection;
import java.util.stream.Collectors;

@AutoValue
public abstract class SchemaBundle {

  public abstract ImmutableSet<GraphQLFieldDefinition> queryFields();

  public abstract ImmutableSet<GraphQLFieldDefinition> mutationFields();

  public abstract ImmutableSet<TypeModification> modifications();

  public abstract ImmutableSet<Descriptors.FileDescriptor> fileDescriptors();

  public abstract ImmutableSet<NodeDataFetcher> nodeDataFetchers();

  public static Builder builder() {
    return new AutoValue_SchemaBundle.Builder();
  }

  public static SchemaBundle combine(Collection<SchemaBundle> schemaBundles) {
    Builder builder = SchemaBundle.builder();
    schemaBundles.forEach(
        schemaBundle -> {
          builder.queryFieldsBuilder().addAll(schemaBundle.queryFields());
          builder.mutationFieldsBuilder().addAll(schemaBundle.mutationFields());
          builder.modificationsBuilder().addAll(schemaBundle.modifications());
          builder.fileDescriptorsBuilder().addAll(schemaBundle.fileDescriptors());
          builder.nodeDataFetchersBuilder().addAll(schemaBundle.nodeDataFetchers());
        });
    return builder.build();
  }

  public static SchemaBundle combineProviders(Collection<Provider<SchemaBundle>> schemaBundles) {
    return combine(
        schemaBundles
            .stream()
            .map(schemaBundleProvider -> schemaBundleProvider.get())
            .collect(ImmutableSet.toImmutableSet()));
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract ImmutableSet.Builder<GraphQLFieldDefinition> queryFieldsBuilder();

    public abstract ImmutableSet.Builder<GraphQLFieldDefinition> mutationFieldsBuilder();

    public abstract ImmutableSet.Builder<TypeModification> modificationsBuilder();

    public abstract ImmutableSet.Builder<Descriptors.FileDescriptor> fileDescriptorsBuilder();

    public abstract ImmutableSet.Builder<NodeDataFetcher> nodeDataFetchersBuilder();

    public abstract SchemaBundle build();
  }
}

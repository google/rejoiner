package com.google.api.graphql.rejoiner;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors;
import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;

@AutoValue
public abstract class SchemaBundle {

  public abstract ImmutableList<GraphQLFieldDefinition> queryFields();

  public abstract ImmutableList<GraphQLFieldDefinition> mutationFields();

  public abstract ImmutableList<TypeModification> modifications();

  public abstract ImmutableSet<Descriptors.FileDescriptor> fileDescriptors();

  public abstract ImmutableList<NodeDataFetcher> nodeDataFetchers();

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

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract ImmutableList.Builder<GraphQLFieldDefinition> queryFieldsBuilder();

    public abstract ImmutableList.Builder<GraphQLFieldDefinition> mutationFieldsBuilder();

    public abstract ImmutableList.Builder<TypeModification> modificationsBuilder();

    public abstract ImmutableSet.Builder<Descriptors.FileDescriptor> fileDescriptorsBuilder();

    public abstract ImmutableList.Builder<NodeDataFetcher> nodeDataFetchersBuilder();

    public abstract SchemaBundle build();
  }
}

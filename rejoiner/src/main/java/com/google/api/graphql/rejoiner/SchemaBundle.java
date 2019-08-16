package com.google.api.graphql.rejoiner;

import static graphql.schema.GraphQLObjectType.newObject;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AutoValue
public abstract class SchemaBundle {

  public GraphQLSchema toSchema() {
    Map<String, ? extends Function<String, Object>> nodeDataFetchers =
        nodeDataFetchers().stream()
            .collect(Collectors.toMap(e -> e.getClassName(), Function.identity()));

    GraphQLObjectType.Builder queryType = newObject().name("QueryType").fields(queryFields());

    ProtoRegistry protoRegistry =
        ProtoRegistry.newBuilder()
            .setComments(commentsMap())
            .addAll(fileDescriptors())
            .add(modifications())
            .build();

    if (protoRegistry.hasRelayNode()) {
      queryType.field(
          new Relay()
              .nodeField(
                  protoRegistry.getRelayNode(),
                  environment -> {
                    String id = environment.getArgument("id");
                    Relay.ResolvedGlobalId resolvedGlobalId = new Relay().fromGlobalId(id);
                    Function<String, ?> stringFunction =
                        nodeDataFetchers.get(resolvedGlobalId.getType());
                    if (stringFunction == null) {
                      throw new RuntimeException(
                          String.format(
                              "Relay Node fetcher not implemented for type=%s",
                              resolvedGlobalId.getType()));
                    }
                    return stringFunction.apply(resolvedGlobalId.getId());
                  }));
    }

    if (mutationFields().isEmpty()) {
      return GraphQLSchema.newSchema()
          .query(queryType)
          .additionalTypes(protoRegistry.listTypes())
          .build();
    }
    GraphQLObjectType mutationType =
        newObject().name("MutationType").fields(mutationFields()).build();
    return GraphQLSchema.newSchema()
        .query(queryType)
        .mutation(mutationType)
        .additionalTypes(protoRegistry.listTypes())
        .build();
  }

  public abstract ImmutableList<GraphQLFieldDefinition> queryFields();

  public abstract ImmutableList<GraphQLFieldDefinition> mutationFields();

  public abstract ImmutableList<TypeModification> modifications();

  public abstract ImmutableSet<Descriptors.FileDescriptor> fileDescriptors();

  public abstract ImmutableList<NodeDataFetcher> nodeDataFetchers();

  public abstract ImmutableMap<String, String> commentsMap();

  public static Builder builder() {
    return new AutoValue_SchemaBundle.Builder();
  }

  public static SchemaBundle combine(Collection<SchemaBundle> schemaBundles) {
    Builder builder = SchemaBundle.builder();
    Map<String, String> commentsMap = new HashMap<>();
    schemaBundles.forEach(
        schemaBundle -> {
          builder.queryFieldsBuilder().addAll(schemaBundle.queryFields());
          builder.mutationFieldsBuilder().addAll(schemaBundle.mutationFields());
          builder.modificationsBuilder().addAll(schemaBundle.modifications());
          builder.fileDescriptorsBuilder().addAll(schemaBundle.fileDescriptors());
          builder.nodeDataFetchersBuilder().addAll(schemaBundle.nodeDataFetchers());
          commentsMap.putAll(schemaBundle.commentsMap());
        });
    builder.commentsMapBuilder().putAll(commentsMap);
    return builder.build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract ImmutableList.Builder<GraphQLFieldDefinition> queryFieldsBuilder();

    public abstract ImmutableList.Builder<GraphQLFieldDefinition> mutationFieldsBuilder();

    public abstract ImmutableList.Builder<TypeModification> modificationsBuilder();

    public abstract ImmutableSet.Builder<Descriptors.FileDescriptor> fileDescriptorsBuilder();

    public abstract ImmutableList.Builder<NodeDataFetcher> nodeDataFetchersBuilder();

    public abstract ImmutableMap.Builder<String, String> commentsMapBuilder();

    public abstract SchemaBundle build();
  }
}

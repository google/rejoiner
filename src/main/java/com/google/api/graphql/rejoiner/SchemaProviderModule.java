package com.google.api.graphql.rejoiner;

import static graphql.schema.GraphQLObjectType.newObject;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.protobuf.Descriptors.FileDescriptor;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;

/** Provides a {@link GraphQLSchema} by combining fields from all SchemaModules. */
public final class SchemaProviderModule extends AbstractModule {

  static class SchemaImpl implements Provider<GraphQLSchema> {
    private final Set<GraphQLFieldDefinition> queryFields;
    private final Set<GraphQLFieldDefinition> mutationFields;
    private final Set<TypeModification> modifications;
    private final Set<FileDescriptor> fileDescriptors;

    @Inject
    public SchemaImpl(
        @Annotations.Queries Set<GraphQLFieldDefinition> queryFields,
        @Annotations.Mutations Set<GraphQLFieldDefinition> mutationFields,
        @Annotations.GraphModifications Set<TypeModification> modifications,
        @Annotations.ExtraTypes Set<FileDescriptor> fileDescriptors) {
      this.queryFields = queryFields;
      this.mutationFields = mutationFields;
      this.modifications = modifications;
      this.fileDescriptors = fileDescriptors;
    }

    @Override
    public GraphQLSchema get() {
      GraphQLObjectType queryType =
          newObject().name("QueryType").fields(Lists.newArrayList(queryFields)).build();
      ProtoRegistry protoRegistry =
          ProtoRegistry.newBuilder().addAll(fileDescriptors).build(modifications);

      if (mutationFields.isEmpty()) {
        return GraphQLSchema.newSchema().query(queryType).build(protoRegistry.listTypes());
      }
      GraphQLObjectType mutationType =
          newObject().name("MutationType").fields(Lists.newArrayList(mutationFields)).build();
      return GraphQLSchema.newSchema()
          .query(queryType)
          .mutation(mutationType)
          .build(protoRegistry.listTypes());
    }
  }

  @Override
  protected void configure() {
    bind(GraphQLSchema.class).annotatedWith(Schema.class).toProvider(SchemaImpl.class);
  }
}

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

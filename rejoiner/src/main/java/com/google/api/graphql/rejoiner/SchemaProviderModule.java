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

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import graphql.relay.Relay;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;

/** Provides a {@link GraphQLSchema} by combining fields from all SchemaModules. */
public final class SchemaProviderModule extends AbstractModule {

  static class SchemaImpl implements Provider<GraphQLSchema> {

    private final Provider<Set<SchemaBundle>> schemaBundleProviders;

    @Inject
    public SchemaImpl(@Annotations.SchemaBundles Provider<Set<SchemaBundle>> schemaBundles) {
      this.schemaBundleProviders = schemaBundles;
    }

    @Override
    public GraphQLSchema get() {
      SchemaBundle schemaBundle = SchemaBundle.combine(schemaBundleProviders.get());
      Map<String, ? extends Function<String, Object>> nodeDataFetchers =
          schemaBundle
              .nodeDataFetchers()
              .stream()
              .collect(Collectors.toMap(e -> e.getClassName(), Function.identity()));

      GraphQLObjectType.Builder queryType =
          newObject().name("QueryType").fields(schemaBundle.queryFields());

      ProtoRegistry protoRegistry =
          ProtoRegistry.newBuilder()
              .addAll(schemaBundle.fileDescriptors())
              .add(schemaBundle.modifications())
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

      if (schemaBundle.mutationFields().isEmpty()) {
        return GraphQLSchema.newSchema().query(queryType).build(protoRegistry.listTypes());
      }
      GraphQLObjectType mutationType =
          newObject().name("MutationType").fields(schemaBundle.mutationFields()).build();
      return GraphQLSchema.newSchema()
          .query(queryType)
          .mutation(mutationType)
          .build(protoRegistry.listTypes());
    }
  }

  @Override
  protected void configure() {
    bind(GraphQLSchema.class)
        .annotatedWith(Schema.class)
        .toProvider(SchemaImpl.class)
        .in(Singleton.class);
  }
}

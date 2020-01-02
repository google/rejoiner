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

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import graphql.schema.GraphQLSchema;
import java.util.Set;
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
      return schemaBundle.toSchema();
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

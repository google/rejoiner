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

package com.google.api.graphql.examples.library.graphqlserver;

import com.google.api.graphql.rejoiner.Schema;
import com.google.api.graphql.rejoiner.SchemaProviderModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import graphql.schema.GraphQLSchema;

final class LibrarySchema {
  static final GraphQLSchema SCHEMA =
      Guice.createInjector(
              new SchemaProviderModule(),
              new BookClientModule(),
              new BookSchemaModule(),
              new ShelfClientModule(),
              new ShelfSchemaModule(),
              new LibrarySchemaModule(),
              new SeedLibrarySchemaModule())
          .getInstance(Key.get(GraphQLSchema.class, Schema.class));
}

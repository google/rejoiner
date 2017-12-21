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

import com.google.api.graphql.rejoiner.Mutation;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.RelayNode;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.example.library.v1.*;

/** A GraphQL {@link SchemaModule} backed by a gRPC service. */
final class BookSchemaModule extends SchemaModule {

  @Query("getBook")
  @RelayNode
  Book getBook(GetBookRequest request, BookServiceGrpc.BookServiceBlockingStub client) {
    return client.getBook(request);
  }

  @Query("listBooks")
  ListBooksResponse listBooks(
      ListBooksRequest request, BookServiceGrpc.BookServiceBlockingStub client) {
    return client.listBooks(request);
  }

  @Mutation("createBook")
  Book createBook(CreateBookRequest request, BookServiceGrpc.BookServiceBlockingStub client) {
    return client.createBook(request);
  }
}

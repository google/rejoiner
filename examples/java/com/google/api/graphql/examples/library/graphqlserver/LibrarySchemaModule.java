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

import com.google.api.graphql.rejoiner.Arg;
import com.google.api.graphql.rejoiner.Mutation;
import com.google.api.graphql.rejoiner.SchemaModification;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.example.library.book.v1.Book;
import com.google.example.library.book.v1.BookServiceGrpc;
import com.google.example.library.book.v1.CreateBookRequest;
import com.google.example.library.book.v1.GetBookRequest;
import com.google.example.library.shelf.v1.GetShelfRequest;
import com.google.example.library.shelf.v1.Shelf;
import com.google.example.library.shelf.v1.ShelfServiceGrpc;
import java.util.List;

/** A GraphQL {@link SchemaModule} backed by a gRPC service. */
final class LibrarySchemaModule extends SchemaModule {

  @Mutation("createBookAndAddToShelf")
  Book createBookAndAddToShelf(
      CreateBookRequest request,
      @Arg("shelf") GetShelfRequest shelfRequest,
      ShelfServiceGrpc.ShelfServiceBlockingStub shelfClient,
      BookServiceGrpc.BookServiceBlockingStub bookClient) {
    Book book = bookClient.createBook(request);
    Shelf shelf = shelfClient.getShelf(shelfRequest);
    // TODO: Add an update shelf RPC
    // shelfClient.updateShelf(shelf.toBuilder().addBookIds(book.getId()).build());
    return book;
  }

  @SchemaModification(addField = "books", onType = Shelf.class)
  ListenableFuture<List<Book>> shelfToBooks(
      Shelf shelf, BookServiceGrpc.BookServiceFutureStub bookClient) {
    // TODO: use a data loader or batch endpoint
    return Futures.allAsList(
        shelf
            .getBookIdsList()
            .stream()
            .map(id -> bookClient.getBook(GetBookRequest.newBuilder().setId(id).build()))
            .collect(ImmutableList.toImmutableList()));
  }
}

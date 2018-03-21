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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.example.library.book.v1.Book;
import com.google.example.library.book.v1.BookServiceGrpc;
import com.google.example.library.book.v1.ListBooksRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import java.util.List;

final class DataLoaderModule extends AbstractModule {

  @Provides
  @RequestScoped
  DataLoaderRegistry dataLoaderRegistry(BookServiceGrpc.BookServiceFutureStub bookService) {

    // TODO: Use multibinder to modularize this, or automate this somehow
    BatchLoader<String, Book> bookBatchLoader =
        keys -> {
          ListenableFuture<List<Book>> listenableFuture =
              Futures.transform(
                  bookService.listBooks(ListBooksRequest.newBuilder().build()),
                  resp -> resp.getBooksList(),
                  MoreExecutors.directExecutor());
          //  ServletScopes.transferRequest(() -> ... ); ??
          return FutureConverter.toCompletableFuture(listenableFuture);
        };

    DataLoaderRegistry registry = new DataLoaderRegistry();

    registry.register("books", new DataLoader<>(bookBatchLoader));
    return registry;
  }
}

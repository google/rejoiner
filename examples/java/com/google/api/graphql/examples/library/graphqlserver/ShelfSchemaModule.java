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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.example.library.shelf.v1.CreateShelfRequest;
import com.google.example.library.shelf.v1.GetShelfRequest;
import com.google.example.library.shelf.v1.ListShelvesRequest;
import com.google.example.library.shelf.v1.ListShelvesResponse;
import com.google.example.library.shelf.v1.MergeShelvesRequest;
import com.google.example.library.shelf.v1.Shelf;
import com.google.example.library.shelf.v1.ShelfServiceGrpc;

/** A GraphQL {@link SchemaModule} backed by a gRPC service. */
final class ShelfSchemaModule extends SchemaModule {

  @Mutation("createShelf")
  ListenableFuture<Shelf> createShelf(
      CreateShelfRequest request, ShelfServiceGrpc.ShelfServiceFutureStub client) {
    return client.createShelf(request);
  }

  @Query("getShelf")
  @RelayNode
  ListenableFuture<Shelf> getShelf(
      GetShelfRequest request, ShelfServiceGrpc.ShelfServiceFutureStub client) {
    return client.getShelf(request);
  }

  @Query("listShelves")
  ListenableFuture<ListShelvesResponse> listShelves(
      ListShelvesRequest request, ShelfServiceGrpc.ShelfServiceFutureStub client) {
    return client.listShelves(request);
  }

  @Mutation("mergeShelves")
  ListenableFuture<Shelf> mergeShelves(
      MergeShelvesRequest request, ShelfServiceGrpc.ShelfServiceFutureStub client) {
    return client.mergeShelves(request);
  }
}

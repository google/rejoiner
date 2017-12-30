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

package com.google.api.graphql.examples.library.shelfbackend;

import com.google.common.collect.ImmutableList;
import com.google.example.library.shelf.v1.CreateShelfRequest;
import com.google.example.library.shelf.v1.DeleteShelfRequest;
import com.google.example.library.shelf.v1.GetShelfRequest;
import com.google.example.library.shelf.v1.ListShelvesRequest;
import com.google.example.library.shelf.v1.ListShelvesResponse;
import com.google.example.library.shelf.v1.Shelf;
import com.google.example.library.shelf.v1.ShelfServiceGrpc;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.Base64;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.GuardedBy;

public class ShelfService extends ShelfServiceGrpc.ShelfServiceImplBase {

  private static final long DEFAULT_PAGE_SIZE = 4;
  private final AtomicInteger shelfIdCounter = new AtomicInteger(0);

  @GuardedBy("this")
  private final TreeMap<String, Shelf> shelfsById = new TreeMap<>();

  @Override
  public synchronized void createShelf(
      CreateShelfRequest request, StreamObserver<Shelf> responseObserver) {
    String id = shelfIdCounter.getAndIncrement() + "";
    shelfsById.put(id, request.getShelf().toBuilder().setId(id).build());
    responseObserver.onNext(shelfsById.get(id));
    responseObserver.onCompleted();
  }

  @Override
  public synchronized void getShelf(
      GetShelfRequest request, StreamObserver<Shelf> responseObserver) {
    responseObserver.onNext(shelfsById.get(request.getId()));
    responseObserver.onCompleted();
  }

  @Override
  public synchronized void deleteShelf(
      DeleteShelfRequest request, StreamObserver<Empty> responseObserver) {
    if (shelfsById.remove(request.getId()) == null) {
      throw new RuntimeException(String.format("Shelf with id=%s not found", request.getId()));
    }
  }

  @Override
  public void listShelves(
      ListShelvesRequest request, StreamObserver<ListShelvesResponse> responseObserver) {
    NavigableMap<String, Shelf> cursor = shelfsById;

    // Resume iteration from the page token.
    if (!request.getPageToken().isEmpty()) {
      String pageToken = decodePageToken(request.getPageToken());
      cursor = cursor.tailMap(pageToken, false);
    }

    ImmutableList<Shelf> shelves =
        cursor
            .values()
            .stream()
            .limit(request.getPageSize() > 0 ? request.getPageSize() : DEFAULT_PAGE_SIZE)
            .collect(ImmutableList.toImmutableList());

    // Return one page of results.
    ListShelvesResponse.Builder responseBuilder =
        ListShelvesResponse.newBuilder().addAllShelves(shelves);
    // Set next page token to resume iteration in the next request.
    if (cursor.values().size() > shelves.size()) {
      String nextPageToken = encodePageToken(shelves.get(shelves.size() - 1).getId());
      responseBuilder.setNextPageToken(nextPageToken);
    }

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  private static String encodePageToken(String token) {
    return new String(Base64.getEncoder().encode(token.getBytes()));
  }

  private static String decodePageToken(String encodedToken) {
    return new String(Base64.getDecoder().decode(encodedToken.getBytes()));
  }
}

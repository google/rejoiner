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

package com.google.api.graphql.examples.library.graphqlserver.client;

import com.google.example.library.book.v1.BookServiceGrpc;
import com.google.inject.AbstractModule;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

final class BookClientModule extends AbstractModule {

  private static final String HOST = "localhost";
  private static final int PORT = 50051;

  @Override
  protected void configure() {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, PORT).usePlaintext().build();
    bind(BookServiceGrpc.BookServiceFutureStub.class)
        .toInstance(BookServiceGrpc.newFutureStub(channel));
    bind(BookServiceGrpc.BookServiceBlockingStub.class)
        .toInstance(BookServiceGrpc.newBlockingStub(channel));
  }
}

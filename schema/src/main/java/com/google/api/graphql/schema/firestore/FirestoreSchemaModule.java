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

package com.google.api.graphql.schema.firestore;

import com.google.api.graphql.rejoiner.GaxSchemaModule;
import com.google.api.graphql.rejoiner.Namespace;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.cloud.firestore.v1beta1.FirestoreClient;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firestore.v1beta1.Document;
import com.google.firestore.v1beta1.GetDocumentRequest;
import com.google.firestore.v1beta1.ListDocumentsRequest;
import com.google.firestore.v1beta1.ListDocumentsResponse;
import graphql.schema.GraphQLFieldDefinition;

import static com.google.api.graphql.schema.FuturesConverter.apiFutureToListenableFuture;

/**
 * A GraphQL {@link SchemaModule} backed by a gRPC service.
 *
 * <p>Calls to this gRPC API is managed by an generated client library, which manages the channel,
 * authentication, etc.
 *
 * <p>https://github.com/googleapis/googleapis/blob/master/google/firestore/v1beta1/firestore.proto
 */
@Namespace("firestore")
public final class FirestoreSchemaModule extends GaxSchemaModule {

  @Query("getDocument")
  ListenableFuture<Document> getDocument(GetDocumentRequest request, FirestoreClient client) {
//     TODO: consider using a dataloader
    return apiFutureToListenableFuture(client.getDocumentCallable().futureCall(request));
  }

  @Query("listDocuments")
  ListenableFuture<ListDocumentsResponse> listDocuments(
      ListDocumentsRequest request, FirestoreClient client) {
    return apiFutureToListenableFuture(client.listDocumentsCallable().futureCall(request));
  }

  @Override
  protected ImmutableList<GraphQLFieldDefinition> extraMutations() {
    return serviceToFields(
        FirestoreClient.class,
        ImmutableList.of("createDocument", "updateDocument", "deleteDocument"));
  }

}

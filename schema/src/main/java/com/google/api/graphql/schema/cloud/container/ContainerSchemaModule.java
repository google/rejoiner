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

package com.google.api.graphql.schema.cloud.container;

import com.google.api.graphql.rejoiner.GaxSchemaModule;
import com.google.api.graphql.rejoiner.Query;
import com.google.cloud.container.v1.ClusterManagerClient;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.container.v1.Cluster;
import com.google.container.v1.GetClusterRequest;
import com.google.container.v1.ListClustersRequest;
import com.google.container.v1.ListClustersResponse;

import static com.google.api.graphql.schema.FuturesConverter.apiFutureToListenableFuture;

public final class ContainerSchemaModule extends GaxSchemaModule {

  @Query("listClusters")
  ListenableFuture<ListClustersResponse> listClusters(
      ClusterManagerClient client, ListClustersRequest request) {
    return apiFutureToListenableFuture(client.listClustersCallable().futureCall(request));
  }

  @Query("getCluster")
  ListenableFuture<Cluster> getCluster(ClusterManagerClient client, GetClusterRequest request) {
    return apiFutureToListenableFuture(client.getClusterCallable().futureCall(request));
  }
}

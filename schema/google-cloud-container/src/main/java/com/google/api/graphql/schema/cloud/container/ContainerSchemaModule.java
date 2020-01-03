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
import com.google.api.graphql.rejoiner.Namespace;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.cloud.container.v1.ClusterManagerClient;
import com.google.common.collect.ImmutableList;

/**
 * A GraphQL {@link SchemaModule} backed by a gRPC service.
 *
 * <p>Calls to this gRPC API is managed by an generated client library, which manages the channel,
 * authentication, etc.
 *
 * <p>https://github.com/googleapis/googleapis/blob/master/google/container/v1/cluster_service.proto
 */
@Namespace("clusterManager")
public final class ContainerSchemaModule extends GaxSchemaModule {

  @Override
  protected void configureSchema() {
    addQueryList(
        serviceToFields(
            ClusterManagerClient.class,
            ImmutableList.of(
                "listClusters",
                "getCluster",
                "listOperations",
                "getOperation",
                "getServerConfig",
                "listNodePools",
                "getNodePool")));
    addMutationList(
        serviceToFields(
            ClusterManagerClient.class,
            ImmutableList.of(
                "createCluster",
                "updateCluster",
                "setNodePoolAutoscaling",
                "setLoggingService",
                "setMonitoringService",
                "setAddonsConfig",
                "setLocations",
                "updateMaster",
                "setMasterAuth",
                "deleteCluster",
                "cancelOperation",
                "createNodePool",
                "deleteNodePool",
                "rollbackNodePoolUpgrade",
                "setNodePoolManagement",
                "setLabels",
                "setLabels",
                "startIPRotation",
                "completeIPRotation",
                "setNodePoolSize",
                "setNetworkPolicy",
                "setMaintenancePolicy")));
  }
}

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

package com.google.api.graphql.rejoiner;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import graphql.relay.Relay;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Registers Protos for building a GraphQL schema. */
final class ProtoRegistry {
  private final BiMap<String, GraphQLType> mapping;
  private final GraphQLInterfaceType nodeInterface;

  private ProtoRegistry(BiMap<String, GraphQLType> mapping, GraphQLInterfaceType nodeInterface) {
    this.mapping = mapping;
    this.nodeInterface = nodeInterface;
  }

  static Builder newBuilder() {
    return new Builder();
  }

  Set<GraphQLType> listTypes() {
    return mapping.values();
  }

  boolean hasRelayNode() {
    return mapping.values().stream()
        .anyMatch(
            type ->
                type instanceof GraphQLObjectType
                    && ((GraphQLObjectType) type).getInterfaces().contains(nodeInterface));
  }

  GraphQLInterfaceType getRelayNode() {
    return nodeInterface;
  }

  static Set<FileDescriptor> extractDependencies(List<FileDescriptor> fileDescriptors) {
    LinkedList<FileDescriptor> loop = new LinkedList<>(fileDescriptors);
    HashSet<FileDescriptor> fileDescriptorSet = new HashSet<>(fileDescriptors);

    while (!loop.isEmpty()) {
      FileDescriptor fileDescriptor = loop.pop();
      for (FileDescriptor dependency : fileDescriptor.getDependencies()) {
        if (!fileDescriptorSet.contains(dependency)) {
          fileDescriptorSet.add(dependency);
          loop.push(dependency);
        }
      }
    }

    return ImmutableSet.copyOf(fileDescriptorSet);
  }
  /** Builder for {@see ProtoRegistry}. */
  public static class Builder {
    private final ArrayList<FileDescriptor> fileDescriptors = new ArrayList<>();
    private final ArrayList<Descriptor> descriptors = new ArrayList<>();
    private final ArrayList<EnumDescriptor> enumDescriptors = new ArrayList<>();
    private final Set<TypeModification> typeModifications = new HashSet<>();
    private SchemaOptions schemaOptions = SchemaOptions.defaultOptions();

    Builder add(FileDescriptor fileDescriptor) {
      fileDescriptors.add(fileDescriptor);
      return this;
    }

    Builder addAll(Collection<FileDescriptor> fileDescriptors) {
      this.fileDescriptors.addAll(fileDescriptors);
      return this;
    }

    Builder add(Descriptor descriptor) {
      descriptors.add(descriptor);
      return this;
    }

    Builder add(EnumDescriptor enumDescriptor) {
      enumDescriptors.add(enumDescriptor);
      return this;
    }

    Builder add(Collection<TypeModification> modifications) {
      typeModifications.addAll(modifications);
      return this;
    }

    ProtoRegistry build() {
      ImmutableListMultimap<String, TypeModification> modificationsMap =
          ImmutableListMultimap.copyOf(
              this.typeModifications.stream()
                  .map(
                      modification ->
                          new SimpleImmutableEntry<>(modification.getTypeName(), modification))
                  .collect(Collectors.toList()));

      final BiMap<String, GraphQLType> mapping = HashBiMap.create();

      GraphQLInterfaceType nodeInterface =
          new Relay()
              .nodeInterface(
                  env -> {
                    Relay.ResolvedGlobalId resolvedGlobalId =
                        new Relay().fromGlobalId(env.getArguments().get("id").toString());
                    return (GraphQLObjectType) mapping.get(resolvedGlobalId.getType());
                  });

      mapping.putAll(
          modifyTypes(
              getMap(fileDescriptors, descriptors, enumDescriptors, nodeInterface, schemaOptions),
              modificationsMap));

      return new ProtoRegistry(mapping, nodeInterface);
    }

    private static BiMap<String, GraphQLType> getMap(
        List<FileDescriptor> fileDescriptors,
        List<Descriptor> descriptors,
        List<EnumDescriptor> enumDescriptors,
        GraphQLInterfaceType nodeInterface,
        SchemaOptions schemaOptions) {
      HashBiMap<String, GraphQLType> mapping =
          HashBiMap.create(getEnumMap(enumDescriptors, schemaOptions));
      LinkedList<Descriptor> loop = new LinkedList<>(descriptors);

      Set<FileDescriptor> fileDescriptorSet = extractDependencies(fileDescriptors);

      for (FileDescriptor fileDescriptor : fileDescriptorSet) {
        loop.addAll(fileDescriptor.getMessageTypes());
        mapping.putAll(getEnumMap(fileDescriptor.getEnumTypes(), schemaOptions));
      }

      while (!loop.isEmpty()) {
        Descriptor descriptor = loop.pop();
        if (!mapping.containsKey(descriptor.getFullName())) {
          mapping.put(
              ProtoToGql.getReferenceName(descriptor),
              ProtoToGql.convert(descriptor, nodeInterface, schemaOptions));
          GqlInputConverter inputConverter =
              GqlInputConverter.newBuilder().add(descriptor.getFile()).build();
          mapping.put(
              GqlInputConverter.getReferenceName(descriptor),
              inputConverter.getInputType(descriptor, schemaOptions));
          loop.addAll(descriptor.getNestedTypes());

          mapping.putAll(getEnumMap(descriptor.getEnumTypes(), schemaOptions));
        }
      }
      return ImmutableBiMap.copyOf(mapping);
    }

    private static BiMap<String, GraphQLType> getEnumMap(
        Iterable<EnumDescriptor> descriptors, SchemaOptions schemaOptions) {
      HashBiMap<String, GraphQLType> mapping = HashBiMap.create();
      for (EnumDescriptor enumDescriptor : descriptors) {
        mapping.put(
            ProtoToGql.getReferenceName(enumDescriptor),
            ProtoToGql.convert(enumDescriptor, schemaOptions));
      }
      return mapping;
    }

    /** Applies the supplied modifications to the GraphQLTypes. */
    private static BiMap<String, GraphQLType> modifyTypes(
        BiMap<String, GraphQLType> mapping,
        ImmutableListMultimap<String, TypeModification> modifications) {
      BiMap<String, GraphQLType> result = HashBiMap.create(mapping.size());
      for (String key : mapping.keySet()) {
        if (mapping.get(key) instanceof GraphQLObjectType) {
          GraphQLObjectType val = (GraphQLObjectType) mapping.get(key);
          if (modifications.containsKey(key)) {
            for (TypeModification modification : modifications.get(key)) {
              val = modification.apply(val);
            }
          }
          result.put(key, val);
        } else {
          result.put(key, mapping.get(key));
        }
      }
      return result;
    }

    public Builder setSchemaOptions(SchemaOptions schemaOptions) {
      this.schemaOptions = schemaOptions;
      return this;
    }
  }
}

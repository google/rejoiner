package com.google.api.graphql.rejoiner;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
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

  private ProtoRegistry(BiMap<String, GraphQLType> mapping) {
    this.mapping = mapping;
  }

  static Builder newBuilder() {
    return new Builder();
  }

  Set<GraphQLType> listTypes() {
    return mapping.values();
  }

  /** Builder for {@see ProtoRegistry}. */
  public static class Builder {
    private final ArrayList<FileDescriptor> fileDescriptors = new ArrayList<>();
    private final ArrayList<Descriptor> descriptors = new ArrayList<>();
    private final ArrayList<EnumDescriptor> enumDescriptors = new ArrayList<>();

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

    ProtoRegistry build() {
      return new ProtoRegistry(getMap(fileDescriptors, descriptors, enumDescriptors));
    }

    ProtoRegistry build(Set<TypeModification> modifications) {
      ImmutableListMultimap<String, TypeModification> modificationsMap =
          ImmutableListMultimap.copyOf(
              modifications
                  .stream()
                  .map(
                      modification ->
                          new SimpleImmutableEntry<>(modification.getTypeName(), modification))
                  .collect(Collectors.toList()));
      return build(modificationsMap);
    }

    private ProtoRegistry build(ImmutableListMultimap<String, TypeModification> modifications) {
      return new ProtoRegistry(
          modifyTypes(getMap(fileDescriptors, descriptors, enumDescriptors), modifications));
    }

    private static BiMap<String, GraphQLType> getMap(
        List<FileDescriptor> fileDescriptors,
        List<Descriptor> descriptors,
        List<EnumDescriptor> enumDescriptors) {
      HashBiMap<String, GraphQLType> mapping = HashBiMap.create(getEnumMap(enumDescriptors));
      LinkedList<Descriptor> loop = new LinkedList<>(descriptors);

      Set<FileDescriptor> fileDescriptorSet = extractDependencies(fileDescriptors);

      for (FileDescriptor fileDescriptor : fileDescriptorSet) {
        loop.addAll(fileDescriptor.getMessageTypes());
        mapping.putAll(getEnumMap(fileDescriptor.getEnumTypes()));
      }

      while (!loop.isEmpty()) {
        Descriptor descriptor = loop.pop();
        if (!mapping.containsKey(descriptor.getFullName())) {
          mapping.put(ProtoToGql.getReferenceName(descriptor), ProtoToGql.convert(descriptor));
          loop.addAll(descriptor.getNestedTypes());

          mapping.putAll(getEnumMap(descriptor.getEnumTypes()));
        }
      }

      return ImmutableBiMap.copyOf(mapping);
    }

    private static Set<FileDescriptor> extractDependencies(List<FileDescriptor> fileDescriptors) {
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

    private static BiMap<String, GraphQLType> getEnumMap(Iterable<EnumDescriptor> descriptors) {
      HashBiMap<String, GraphQLType> mapping = HashBiMap.create();
      for (EnumDescriptor enumDescriptor : descriptors) {
        mapping.put(
            ProtoToGql.getReferenceName(enumDescriptor), ProtoToGql.convert(enumDescriptor));
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
  }
}

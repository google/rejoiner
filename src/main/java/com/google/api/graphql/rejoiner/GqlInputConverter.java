package com.google.api.graphql.rejoiner;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.protobuf.Message;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Converts GraphQL inputs into Protobuf message. */
public final class GqlInputConverter {

  private final BiMap<String, Descriptor> descriptorMapping;
  private final BiMap<String, EnumDescriptor> enumMapping;

  private GqlInputConverter(
      BiMap<String, Descriptor> descriptorMapping, BiMap<String, EnumDescriptor> enumMapping) {
    this.descriptorMapping = descriptorMapping;
    this.enumMapping = enumMapping;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Message createProtoBuf(
      Descriptor descriptor, Message.Builder builder, Map<String, Object> input) {

    if (input == null) {
      return builder.build();
    }
    for (FieldDescriptor field : descriptor.getFields()) {
      String fieldName = field.getName();

      if (!input.containsKey(fieldName)) {
        // TODO: validate required fields
        continue;
      }

      if (field.isRepeated()) {
        List<Object> values = (List<Object>) input.get(fieldName);
        for (Object value : values) {
          builder.addRepeatedField(field, getValueForField(field, value, builder));
        }
      } else {
        builder.setField(field, getValueForField(field, input.get(fieldName), builder));
      }
    }

    return builder.build();
  }

  Object getValueForField(FieldDescriptor field, Object value, Message.Builder builder) {
    // TODO: handle groups, oneof
    if (field.getType() == FieldDescriptor.Type.MESSAGE) {
      Descriptor fieldTypeDescriptor =
          descriptorMapping.get(getReferenceName(field.getMessageType()));
      return createProtoBuf(
          fieldTypeDescriptor, builder.newBuilderForField(field), (Map<String, Object>) value);
    }

    if (field.getType() == FieldDescriptor.Type.ENUM) {
      EnumDescriptor enumDescriptor = enumMapping.get(getReferenceName(field.getEnumType()));
      return enumDescriptor.findValueByNumber((int) value);
    }

    return value;
  }

  public GraphQLArgument createArgument(Descriptor descriptor, String name) {
    return GraphQLArgument.newArgument().name(name).type(createCustomType(descriptor)).build();
  }

  public GraphQLArgument createRequiredArgument(Descriptor descriptor, String name) {
    return GraphQLArgument.newArgument()
        .name(name)
        .type(new GraphQLNonNull(createCustomType(descriptor)))
        .build();
  }

  public GraphQLArgument createArgumentForField(FieldDescriptor fieldDescriptor) {
    // TODO: only works for non-repeated fields
    return GraphQLArgument.newArgument()
        .name(fieldDescriptor.getName())
        .type((GraphQLInputType) getInputType(fieldDescriptor))
        .build();
  }

  private GraphQLInputObjectType createCustomType(Descriptor descriptor) {
    // TODO: This may not handle self-references correctly,
    // resulting in infinite recursion
    GraphQLInputObjectType.Builder builder =
        GraphQLInputObjectType.newInputObject().name(getReferenceName(descriptor));

    for (FieldDescriptor field : descriptor.getFields()) {
      GraphQLType fieldType = getInputType(field);

      GraphQLInputObjectField.Builder inputBuilder =
          GraphQLInputObjectField.newInputObjectField().name(field.getName());
      if (field.isRepeated()) {
        inputBuilder.type(new GraphQLList(fieldType));
      } else {
        inputBuilder.type((GraphQLInputType) fieldType);
      }

      builder.field(inputBuilder.build());
    }
    return builder.build();
  }

  private GraphQLType getInputType(FieldDescriptor field) {
    if (field.getType() == FieldDescriptor.Type.MESSAGE
        || field.getType() == FieldDescriptor.Type.GROUP) {
      String fieldTypeName = getReferenceName(field.getMessageType());
      return createCustomType(descriptorMapping.get(fieldTypeName));
    }
    if (field.getType() == FieldDescriptor.Type.ENUM) {
      String enumTypeName = getReferenceName(field.getEnumType());
      return createEnumType(enumMapping.get(enumTypeName));
    }
    GraphQLType type = ProtoToGql.convertType(field);
    if (type instanceof GraphQLList) {
      return ((GraphQLList) type).getWrappedType();
    }
    return type;
  }

  private static String getReferenceName(GenericDescriptor descriptor) {
    return "Input_" + ProtoToGql.getReferenceName(descriptor);
  }

  private static GraphQLEnumType createEnumType(EnumDescriptor descriptor) {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name(getReferenceName(descriptor));
    for (Descriptors.EnumValueDescriptor valueDescriptor : descriptor.getValues()) {
      builder.value(valueDescriptor.getName(), valueDescriptor.getNumber());
    }
    return builder.build();
  }

  // Based on ProtoRegistry.Builder, but builds a map of descriptors rather than types.
  /** Builder for GqlInputConverter. */
  public static class Builder {
    private final ArrayList<FileDescriptor> fileDescriptors = new ArrayList<>();
    private final ArrayList<Descriptor> descriptors = new ArrayList<>();
    private final ArrayList<EnumDescriptor> enumDescriptors = new ArrayList<>();

    public Builder add(FileDescriptor fileDescriptor) {
      fileDescriptors.add(fileDescriptor);
      return this;
    }

    public Builder add(Descriptor descriptor) {
      descriptors.add(descriptor);
      return this;
    }

    public Builder add(EnumDescriptor enumDescriptor) {
      enumDescriptors.add(enumDescriptor);
      return this;
    }

    public GqlInputConverter build() {
      HashBiMap<String, Descriptor> mapping = HashBiMap.create();
      HashBiMap<String, EnumDescriptor> enumMapping = HashBiMap.create(getEnumMap(enumDescriptors));
      LinkedList<Descriptor> loop = new LinkedList<>(descriptors);

      Set<FileDescriptor> fileDescriptorSet = extractDependencies(fileDescriptors);

      for (FileDescriptor fileDescriptor : fileDescriptorSet) {
        loop.addAll(fileDescriptor.getMessageTypes());
        enumMapping.putAll(getEnumMap(fileDescriptor.getEnumTypes()));
      }

      while (!loop.isEmpty()) {
        Descriptor descriptor = loop.pop();
        if (!mapping.containsKey(descriptor.getFullName())) {
          mapping.put(getReferenceName(descriptor), descriptor);
          loop.addAll(descriptor.getNestedTypes());
          enumMapping.putAll(getEnumMap(descriptor.getEnumTypes()));
        }
      }

      return new GqlInputConverter(
          ImmutableBiMap.copyOf(mapping), ImmutableBiMap.copyOf(enumMapping));
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

    private static BiMap<String, EnumDescriptor> getEnumMap(Iterable<EnumDescriptor> descriptors) {
      HashBiMap<String, EnumDescriptor> mapping = HashBiMap.create();
      for (EnumDescriptor enumDescriptor : descriptors) {
        mapping.put(getReferenceName(enumDescriptor), enumDescriptor);
      }
      return mapping;
    }
  }
}

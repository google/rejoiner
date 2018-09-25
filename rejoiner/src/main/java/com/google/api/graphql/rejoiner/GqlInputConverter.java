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

import static graphql.Scalars.GraphQLString;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.protobuf.Message;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts GraphQL inputs into Protobuf message.
 *
 * <p>Keeps a mapping from type name to Proto descriptor for Message and Enum types.
 */
final class GqlInputConverter {

  private final BiMap<String, Descriptor> descriptorMapping;
  private final BiMap<String, EnumDescriptor> enumMapping;

  private static final Converter<String, String> UNDERSCORE_TO_CAMEL =
      CaseFormat.LOWER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL);

  private GqlInputConverter(
      BiMap<String, Descriptor> descriptorMapping, BiMap<String, EnumDescriptor> enumMapping) {
    this.descriptorMapping = descriptorMapping;
    this.enumMapping = enumMapping;
  }

  static Builder newBuilder() {
    return new Builder();
  }

  Message createProtoBuf(
      Descriptor descriptor, Message.Builder builder, Map<String, Object> input) {

    if (input == null) {
      return builder.build();
    }

    Map<String, Object> remainingInput = new HashMap<>(input);
    for (FieldDescriptor field : descriptor.getFields()) {
      String fieldName = getFieldName(field);

      if (!remainingInput.containsKey(fieldName)) {
        // TODO: validate required fields
        continue;
      }

      if (field.isRepeated()) {
        List<Object> values = (List<Object>) remainingInput.remove(fieldName);
        for (Object value : values) {
          builder.addRepeatedField(field, getValueForField(field, value, builder));
        }
      } else {
        builder.setField(field, getValueForField(field, remainingInput.remove(fieldName), builder));
      }
    }

    if (!remainingInput.isEmpty()) {
      throw new AssertionError(
          "All fields in input should have been consumed. Remaining: " + remainingInput);
    }

    return builder.build();
  }

  GraphQLType getInputType(Descriptor descriptor) {
    GraphQLInputObjectType.Builder builder =
        GraphQLInputObjectType.newInputObject().name(getReferenceName(descriptor));

    if (descriptor.getFields().isEmpty()) {
      builder.field(STATIC_FIELD);
    }
    for (FieldDescriptor field : descriptor.getFields()) {
      GraphQLType fieldType = getFieldType(field);
      GraphQLInputObjectField.Builder inputBuilder =
          GraphQLInputObjectField.newInputObjectField().name(getFieldName(field));
      if (field.isRepeated()) {
        inputBuilder.type(new GraphQLList(fieldType));
      } else {
        inputBuilder.type((GraphQLInputType) fieldType);
      }

      builder.field(inputBuilder.build());
    }
    return builder.build();
  }

  private Object getValueForField(FieldDescriptor field, Object value, Message.Builder builder) {
    // TODO: handle groups, oneof
    if (field.getType() == FieldDescriptor.Type.MESSAGE) {
      Descriptor fieldTypeDescriptor =
          descriptorMapping.get(getReferenceName(field.getMessageType()));
      return createProtoBuf(
          fieldTypeDescriptor, builder.newBuilderForField(field), (Map<String, Object>) value);
    }

    if (field.getType() == FieldDescriptor.Type.ENUM) {
      EnumDescriptor enumDescriptor =
          enumMapping.get(ProtoToGql.getReferenceName(field.getEnumType()));
      return enumDescriptor.findValueByName(value.toString());
    }

    return value;
  }

  static GraphQLArgument createArgument(Descriptor descriptor, String name) {
    return GraphQLArgument.newArgument().name(name).type(getInputTypeReference(descriptor)).build();
  }

  static String getReferenceName(GenericDescriptor descriptor) {
    return "Input_" + ProtoToGql.getReferenceName(descriptor);
  }

  /** Field names with under_scores are converted to camelCase. */
  private String getFieldName(FieldDescriptor field) {
    String fieldName = field.getName();
    return fieldName.contains("_") ? UNDERSCORE_TO_CAMEL.convert(fieldName) : fieldName;
  }

  private GraphQLType getFieldType(FieldDescriptor field) {
    if (field.getType() == FieldDescriptor.Type.MESSAGE
        || field.getType() == FieldDescriptor.Type.GROUP) {
      return new GraphQLTypeReference(getReferenceName(field.getMessageType()));
    }
    if (field.getType() == FieldDescriptor.Type.ENUM) {
      return new GraphQLTypeReference(ProtoToGql.getReferenceName(field.getEnumType()));
    }
    GraphQLType type = ProtoToGql.convertType(field);
    if (type instanceof GraphQLList) {
      return ((GraphQLList) type).getWrappedType();
    }
    return type;
  }

  private static GraphQLInputType getInputTypeReference(Descriptor descriptor) {
    return new GraphQLTypeReference(getReferenceName(descriptor));
  }

  private static final GraphQLInputObjectField STATIC_FIELD =
      GraphQLInputObjectField.newInputObjectField()
          .type(GraphQLString)
          .name("_")
          .defaultValue("")
          .description("NOT USED")
          .build();

  // Based on ProtoRegistry.Builder, but builds a map of descriptors rather than types.

  /** Builder for GqlInputConverter. */
  static class Builder {
    private final ArrayList<FileDescriptor> fileDescriptors = new ArrayList<>();
    private final ArrayList<Descriptor> descriptors = new ArrayList<>();
    private final ArrayList<EnumDescriptor> enumDescriptors = new ArrayList<>();

    Builder add(FileDescriptor fileDescriptor) {
      fileDescriptors.add(fileDescriptor);
      return this;
    }

    GqlInputConverter build() {
      HashBiMap<String, Descriptor> mapping = HashBiMap.create();
      HashBiMap<String, EnumDescriptor> enumMapping = HashBiMap.create(getEnumMap(enumDescriptors));
      LinkedList<Descriptor> loop = new LinkedList<>(descriptors);

      Set<FileDescriptor> fileDescriptorSet = ProtoRegistry.extractDependencies(fileDescriptors);

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

    private static BiMap<String, EnumDescriptor> getEnumMap(Iterable<EnumDescriptor> descriptors) {
      HashBiMap<String, EnumDescriptor> mapping = HashBiMap.create();
      for (EnumDescriptor enumDescriptor : descriptors) {
        mapping.put(ProtoToGql.getReferenceName(enumDescriptor), enumDescriptor);
      }
      return mapping;
    }
  }
}

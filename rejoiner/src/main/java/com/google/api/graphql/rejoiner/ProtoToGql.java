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

import com.google.api.graphql.grpc.ProtoScalars;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Descriptors.GenericDescriptor;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

/** Converts Protos to GraphQL Types. */
final class ProtoToGql {

  private ProtoToGql() {}

  private static final ImmutableMap<Type, GraphQLScalarType> PROTO_TYPE_MAP =
      new ImmutableMap.Builder<Type, GraphQLScalarType>()
          .put(Type.BOOL, Scalars.GraphQLBoolean)
          .put(Type.FLOAT, Scalars.GraphQLFloat)
          .put(Type.INT32, Scalars.GraphQLInt)
          .put(Type.INT64, Scalars.GraphQLLong)
          .put(Type.STRING, Scalars.GraphQLString)
          .put(Type.DOUBLE, Scalars.GraphQLFloat)
          .put(Type.UINT32, ProtoScalars.UINT_32)
          .put(Type.UINT64, ProtoScalars.UINT_64)
          .put(Type.SINT32, ProtoScalars.SINT_32)
          .put(Type.SINT64, ProtoScalars.SINT_64)
          .put(Type.BYTES, ProtoScalars.BYTES)
          .put(Type.FIXED32, ProtoScalars.FIXED_32)
          .put(Type.FIXED64, ProtoScalars.FIXED_64)
          .put(Type.SFIXED32, ProtoScalars.S_FIXED_32)
          .put(Type.SFIXED64, ProtoScalars.S_FIXED_64)
          .build();

  private static final ImmutableMap<Type, GraphQLScalarType> TYPE_MAP =
      new ImmutableMap.Builder<Type, GraphQLScalarType>()
          .put(Type.BOOL, Scalars.GraphQLBoolean)
          .put(Type.FLOAT, Scalars.GraphQLFloat)
          .put(Type.INT32, Scalars.GraphQLInt)
          .put(Type.INT64, Scalars.GraphQLLong)
          .put(Type.STRING, Scalars.GraphQLString)
          .put(Type.DOUBLE, Scalars.GraphQLFloat)
          .put(Type.UINT32, Scalars.GraphQLInt)
          .put(Type.UINT64, Scalars.GraphQLLong)
          .put(Type.SINT32, Scalars.GraphQLInt)
          .put(Type.SINT64, Scalars.GraphQLLong)
          .put(Type.BYTES, Scalars.GraphQLString)
          .put(Type.FIXED32, Scalars.GraphQLInt)
          .put(Type.FIXED64, Scalars.GraphQLLong)
          .put(Type.SFIXED32, Scalars.GraphQLInt)
          .put(Type.SFIXED64, Scalars.GraphQLLong)
          .build();

  private static final ImmutableList<GraphQLFieldDefinition> STATIC_FIELD =
      ImmutableList.of(newFieldDefinition().type(GraphQLString).name("_").staticValue("-").build());

  private static GraphQLFieldDefinition convertField(
      FieldDescriptor fieldDescriptor, SchemaOptions schemaOptions) {
    DataFetcher<?> dataFetcher = new ProtoDataFetcher(fieldDescriptor);
    GraphQLFieldDefinition.Builder builder =
        newFieldDefinition()
            .type(convertType(fieldDescriptor, schemaOptions))
            .dataFetcher(dataFetcher)
            .name(fieldDescriptor.getJsonName());
    builder.description(schemaOptions.commentsMap().get(fieldDescriptor.getFullName()));
    if (fieldDescriptor.getOptions().hasDeprecated()
        && fieldDescriptor.getOptions().getDeprecated()) {
      builder.deprecate("deprecated in proto");
    }
    return builder.build();
  }

  /** Returns a GraphQLOutputType generated from a FieldDescriptor. */
  static GraphQLOutputType convertType(
      FieldDescriptor fieldDescriptor, SchemaOptions schemaOptions) {
    final GraphQLOutputType type;

    if (fieldDescriptor.getType() == Type.MESSAGE) {
      type = getReference(fieldDescriptor.getMessageType());
    } else if (fieldDescriptor.getType() == Type.GROUP) {
      type = getReference(fieldDescriptor.getMessageType());
    } else if (fieldDescriptor.getType() == Type.ENUM) {
      type = getReference(fieldDescriptor.getEnumType());
    } else if (schemaOptions.useProtoScalarTypes()) {
      type = PROTO_TYPE_MAP.get(fieldDescriptor.getType());
    } else {
      type = TYPE_MAP.get(fieldDescriptor.getType());
    }

    if (type == null) {
      throw new RuntimeException("Unknown type: " + fieldDescriptor.getType());
    }

    if (fieldDescriptor.isRepeated()) {
      return new GraphQLList(type);
    } else {
      return type;
    }
  }

  static GraphQLObjectType convert(
      Descriptor descriptor,
      GraphQLInterfaceType nodeInterface,
      SchemaOptions schemaOptions) {
    ImmutableList<GraphQLFieldDefinition> graphQLFieldDefinitions =
        descriptor.getFields().stream()
            .map(field -> ProtoToGql.convertField(field, schemaOptions))
            .collect(toImmutableList());

    // TODO: add back relay support

    //    Optional<GraphQLFieldDefinition> relayId =
    //        descriptor.getFields().stream()
    //            .filter(field -> field.getOptions().hasExtension(RelayOptionsProto.relayOptions))
    //            .map(
    //                field ->
    //                    newFieldDefinition()
    //                        .name("id")
    //                        .type(new GraphQLNonNull(GraphQLID))
    //                        .description("Relay ID")
    //                        .dataFetcher(
    //                            data ->
    //                                new Relay()
    //                                    .toGlobalId(
    //                                        getReferenceName(descriptor),
    //                                        data.<Message>getSource().getField(field).toString()))
    //                        .build())
    //            .findFirst();

    //   if (relayId.isPresent()) {
    //      return GraphQLObjectType.newObject()
    //          .name(getReferenceName(descriptor))
    //          .withInterface(nodeInterface)
    //          .field(relayId.get())
    //          .fields(
    //              graphQLFieldDefinitions
    //                  .stream()
    //                  .map(
    //                      field ->
    //                          field.getName().equals("id")
    //                              ? GraphQLFieldDefinition.newFieldDefinition()
    //                                  .name("rawId")
    //                                  .description(field.getDescription())
    //                                  .type(field.getType())
    //                                  .dataFetcher(field.getDataFetcher())
    //                                  .build()
    //                              : field)
    //                  .collect(ImmutableList.toImmutableList()))
    //          .build();
    //    }

    return GraphQLObjectType.newObject()
        .name(getReferenceName(descriptor))
        .description(schemaOptions.commentsMap().get(descriptor.getFullName()))
        .fields(graphQLFieldDefinitions.isEmpty() ? STATIC_FIELD : graphQLFieldDefinitions)
        .build();
  }

  static GraphQLEnumType convert(
      EnumDescriptor descriptor, SchemaOptions schemaOptions) {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name(getReferenceName(descriptor));
    for (EnumValueDescriptor value : descriptor.getValues()) {
      builder.value(
          value.getName(),
          value.getName(),
          schemaOptions.commentsMap().get(value.getFullName()),
          value.getOptions().getDeprecated() ? "deprecated in proto" : null);
    }
    return builder.build();
  }

  /** Returns the GraphQL name of the supplied proto. */
  static String getReferenceName(GenericDescriptor descriptor) {
    return CharMatcher.anyOf(".").replaceFrom(descriptor.getFullName(), "_");
  }

  /** Returns a reference to the GraphQL type corresponding to the supplied proto. */
  static GraphQLTypeReference getReference(GenericDescriptor descriptor) {
    return new GraphQLTypeReference(getReferenceName(descriptor));
  }
}

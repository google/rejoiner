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

import com.google.api.graphql.options.RelayOptionsProto;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.protobuf.Message;
import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

/** Converts Protos to GraphQL Types. */
final class ProtoToGql {

  private ProtoToGql() {}

  private static final ImmutableMap<Type, GraphQLScalarType> TYPE_MAP =
      new ImmutableMap.Builder<Type, GraphQLScalarType>()
          .put(Type.BOOL, Scalars.GraphQLBoolean)
          .put(Type.FLOAT, Scalars.GraphQLFloat)
          .put(Type.INT32, Scalars.GraphQLInt)
          .put(Type.INT64, Scalars.GraphQLLong)
          .put(Type.STRING, Scalars.GraphQLString)
          // TODO: Add additional Scalar types to GraphQL
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

  private static final Converter<String, String> UNDERSCORE_TO_CAMEL =
      CaseFormat.LOWER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL);
  private static final Converter<String, String> LOWER_CAMEL_TO_UPPER =
      CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);
  private static final ImmutableList<GraphQLFieldDefinition> STATIC_FIELD =
      ImmutableList.of(newFieldDefinition().type(GraphQLString).name("_").staticValue("-").build());

  private static GraphQLFieldDefinition convertField(
      FieldDescriptor fieldDescriptor, ImmutableMap<String, String> commentsMap) {
    final String fieldName = fieldDescriptor.getName();
    final String convertedFieldName =
        fieldName.contains("_") ? UNDERSCORE_TO_CAMEL.convert(fieldName) : fieldName;
    final String methodNameSuffix =
        fieldDescriptor.isMapField() ? "Map" : fieldDescriptor.isRepeated() ? "List" : "";
    final String methodName =
        "get" + LOWER_CAMEL_TO_UPPER.convert(convertedFieldName) + methodNameSuffix;
    final ProtoDataFetcher protoDataFetcher = new ProtoDataFetcher(methodName);
    final DataFetcher dataFetcher =
        !fieldDescriptor.isMapField()
            ? protoDataFetcher
            : (DataFetchingEnvironment environment) -> {
              final Map<Object, Object> field = (Map) protoDataFetcher.get(environment);
              return field.entrySet().stream()
                  .map(entry -> ImmutableMap.of("key", entry.getKey(), "value", entry.getValue()))
                  .collect(toImmutableList());
            };
    GraphQLFieldDefinition.Builder builder =
        newFieldDefinition()
            .type(convertType(fieldDescriptor))
            .dataFetcher(dataFetcher)
            .name(fieldDescriptor.getJsonName());
    builder.description(commentsMap.get(fieldDescriptor.getFullName()));
    if (fieldDescriptor.getOptions().hasDeprecated()
        && fieldDescriptor.getOptions().getDeprecated()) {
      builder.deprecate("deprecated in proto");
    }
    return builder.build();
  }

  private static class ProtoDataFetcher implements DataFetcher<Object> {
    private final String methodName;
    private Method method = null;

    ProtoDataFetcher(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
      final Object source = environment.getSource();
      if (source == null) return null;
      if (method == null)
        // no synchronization necessary because this line is idempotent
        method = source.getClass().getMethod(methodName);
      return method.invoke(source);
    }
  }

  /** Returns a GraphQLOutputType generated from a FieldDescriptor. */
  static GraphQLOutputType convertType(FieldDescriptor fieldDescriptor) {
    final GraphQLOutputType type;

    if (fieldDescriptor.getType() == Type.MESSAGE) {
      type = getReference(fieldDescriptor.getMessageType());
    } else if (fieldDescriptor.getType() == Type.GROUP) {
      type = getReference(fieldDescriptor.getMessageType());
    } else if (fieldDescriptor.getType() == Type.ENUM) {
      type = getReference(fieldDescriptor.getEnumType());
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
      ImmutableMap<String, String> commentsMap) {
    ImmutableList<GraphQLFieldDefinition> graphQLFieldDefinitions =
        descriptor.getFields().stream()
            .map(field -> ProtoToGql.convertField(field, commentsMap))
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
        .description(commentsMap.get(descriptor.getFullName()))
        .fields(graphQLFieldDefinitions.isEmpty() ? STATIC_FIELD : graphQLFieldDefinitions)
        .build();
  }

  static GraphQLEnumType convert(
      EnumDescriptor descriptor, ImmutableMap<String, String> commentsMap) {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name(getReferenceName(descriptor));
    for (EnumValueDescriptor value : descriptor.getValues()) {
      builder.value(
          value.getName(),
          value.getName(),
          commentsMap.get(value.getFullName()),
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

  //  private java.util.function.Function<Object, Object> getter(Class clazz, String methodName) {
  //    MethodHandles.Lookup lookup = MethodHandles.lookup();
  //
  //    try {
  //      CallSite site = LambdaMetafactory.metafactory(lookup,
  //
  //              "apply",
  //
  //              MethodType.methodType(Function.class),
  //
  //              MethodType.methodType(Object.class, Object.class),
  //
  //              lookup.findVirtual(clazz, methodName, MethodType.methodType(String.class)),
  //
  //              MethodType.methodType(Object.class, clazz));
  //      return (java.util.function.Function<Object, Object>) site.getTarget().invokeExact();
  //    } catch (LambdaConversionException e) {
  //      e.printStackTrace();
  //    } catch (NoSuchMethodException e) {
  //      e.printStackTrace();
  //    } catch (IllegalAccessException e) {
  //      e.printStackTrace();
  //    } catch (Throwable throwable) {
  //      throwable.printStackTrace();
  //    }
  //
  //  }
}

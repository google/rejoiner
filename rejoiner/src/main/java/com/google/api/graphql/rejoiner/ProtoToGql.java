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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import com.google.api.graphql.options.RelayOptionsProto;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Converter;
import com.google.common.base.Function;
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
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

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

  private static final Converter<String, String> LOWER_CAMEL_TO_UPPER =
      CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);
  private static final FieldConverter FIELD_CONVERTER = new FieldConverter();
  private static final ImmutableList<GraphQLFieldDefinition> STATIC_FIELD =
      ImmutableList.of(newFieldDefinition().type(GraphQLString).name("_").staticValue("-").build());

  private static class FieldConverter implements Function<FieldDescriptor, GraphQLFieldDefinition> {

    private static class ProtoDataFetcher implements DataFetcher {

      private final String name;

      private ProtoDataFetcher(String name) {
        this.name = name;
      }

      @Override
      public Object get(DataFetchingEnvironment environment) {
        Object source = environment.getSource();
        if (source == null) {
          return null;
        }
        if (source instanceof Map) {
          return ((Map<?, ?>) source).get(name);
        }
        GraphQLType type = environment.getFieldType();
        if (type instanceof GraphQLNonNull) {
          type = ((GraphQLNonNull) type).getWrappedType();
        }
        if (type instanceof GraphQLList) {

          Object listValue = call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name) + "List");
          if (listValue != null) {
            return listValue;
          }
          Object mapValue = call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name) + "Map");
          if (mapValue == null) {
            return null;
          }
          Map<?, ?> map = (Map<?, ?>) mapValue;
          return map.entrySet().stream().map(entry -> ImmutableMap.of("key", entry.getKey(), "value", entry.getValue())).collect(toImmutableList());
        }
        if (type instanceof GraphQLEnumType) {
          Object o = call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name));
          if (o != null) {
            return o.toString();
          }
        }

        return call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name));
      }

      private static Object call(Object object, String methodName) {
        try {
          Method method = object.getClass().getMethod(methodName);
          return method.invoke(object);
        } catch (NoSuchMethodException e) {
          return null;
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public GraphQLFieldDefinition apply(FieldDescriptor fieldDescriptor) {
      String fieldName = fieldDescriptor.getJsonName();
      GraphQLFieldDefinition.Builder builder =
          GraphQLFieldDefinition.newFieldDefinition()
              .type(convertType(fieldDescriptor))
              .dataFetcher(
                  new ProtoDataFetcher(fieldName))
              .name(fieldName);
      if (fieldDescriptor.getFile().toProto().getSourceCodeInfo().getLocationCount()
          > fieldDescriptor.getIndex()) {
        builder.description(
            fieldDescriptor
                .getFile()
                .toProto()
                .getSourceCodeInfo()
                .getLocation(fieldDescriptor.getIndex())
                .getLeadingComments());
      }
      if (fieldDescriptor.getOptions().hasDeprecated()
          && fieldDescriptor.getOptions().getDeprecated()) {
        builder.deprecate("deprecated in proto");
      }
      return builder.build();
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

  static GraphQLObjectType convert(Descriptor descriptor, GraphQLInterfaceType nodeInterface) {
    ImmutableList<GraphQLFieldDefinition> graphQLFieldDefinitions =
        descriptor.getFields().stream().map(FIELD_CONVERTER).collect(toImmutableList());

    Optional<GraphQLFieldDefinition> relayId =
        descriptor
            .getFields()
            .stream()
            .filter(field -> field.getOptions().hasExtension(RelayOptionsProto.relayOptions))
            .map(
                field ->
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(GraphQLID))
                        .description("Relay ID")
                        .dataFetcher(
                            data ->
                                new Relay()
                                    .toGlobalId(
                                        getReferenceName(descriptor),
                                        data.<Message>getSource().getField(field).toString()))
                        .build())
            .findFirst();

    if (relayId.isPresent()) {
      return GraphQLObjectType.newObject()
          .name(getReferenceName(descriptor))
          .withInterface(nodeInterface)
          .field(relayId.get())
          .fields(
              graphQLFieldDefinitions
                  .stream()
                  .map(
                      field ->
                          field.getName().equals("id")
                              ? GraphQLFieldDefinition.newFieldDefinition()
                                  .name("rawId")
                                  .description(field.getDescription())
                                  .type(field.getType())
                                  .dataFetcher(field.getDataFetcher())
                                  .build()
                              : field)
                  .collect(ImmutableList.toImmutableList()))
          .build();
    }

    return GraphQLObjectType.newObject()
        .name(getReferenceName(descriptor))
        .fields(graphQLFieldDefinitions.isEmpty() ? STATIC_FIELD : graphQLFieldDefinitions)
        .build();
  }

  static GraphQLEnumType convert(EnumDescriptor descriptor) {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name(getReferenceName(descriptor));
    for (EnumValueDescriptor value : descriptor.getValues()) {
      builder.value(value.getName());
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

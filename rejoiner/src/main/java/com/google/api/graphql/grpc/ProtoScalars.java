package com.google.api.graphql.grpc;

import graphql.Scalars;
import graphql.schema.GraphQLScalarType;

/**
 * Custom GraphQL scalars for protocol-buffer scalar types.
 *
 * <p>To learn about proto scalars see:
 * https://developers.google.com/protocol-buffers/docs/proto3#scalar
 *
 * <p>To learn about GraphQL scalars see http://graphql.org/learn/schema/#scalar-types and
 * http://facebook.github.io/graphql/#sec-Scalars
 */
public final class ProtoScalars {

  private ProtoScalars() {}

  public static final GraphQLScalarType UINT_32 =
      GraphQLScalarType.newScalar(Scalars.GraphQLInt)
          .name("uint32")
          .description("Scalar for proto type uint32. Uses variable-length encoding.")
          .build();

  public static final GraphQLScalarType UINT_64 =
      GraphQLScalarType.newScalar(Scalars.GraphQLLong)
          .name("uint64")
          .description("Scalar for proto type uint64. Uses variable-length encoding.")
          .build();

  public static final GraphQLScalarType SINT_32 =
      GraphQLScalarType.newScalar(Scalars.GraphQLInt)
          .name("sint32")
          .description(
              "Scalar for proto type sint32. Uses variable-length encoding."
                  + " Signed int value. These more efficiently encode negative numbers than regular int32s.")
          .build();

  public static final GraphQLScalarType SINT_64 =
      GraphQLScalarType.newScalar(Scalars.GraphQLLong)
          .name("sint64")
          .description(
              "Scalar for proto type sint64. Uses variable-length encoding. Signed int value."
                  + " These more efficiently encode negative numbers than regular int64s.")
          .build();

  public static final GraphQLScalarType FIXED_32 =
      GraphQLScalarType.newScalar(Scalars.GraphQLLong)
          .name("fixed32")
          .description(
              "Scalar for proto type fixed32. Always four bytes."
                  + " More efficient than uint32 if values are often greater than 2^28.")
          .build();

  public static final GraphQLScalarType FIXED_64 =
      GraphQLScalarType.newScalar(Scalars.GraphQLLong)
          .name("fixed64")
          .description(
              "Scalar for proto type fixed64. Always eight bytes."
                  + " More efficient than uint64 if values are often greater than 2^56.")
          .build();

  public static final GraphQLScalarType S_FIXED_32 =
      GraphQLScalarType.newScalar(Scalars.GraphQLInt)
          .name("sfixed32")
          .description("Scalar for proto type sfixed32. Always four bytes.")
          .build();

  public static final GraphQLScalarType S_FIXED_64 =
      GraphQLScalarType.newScalar(Scalars.GraphQLLong)
          .name("sfixed64")
          .description("Scalar for proto type sfixed64. Always eight bytes.")
          .build();

  public static final GraphQLScalarType BYTES =
      GraphQLScalarType.newScalar(Scalars.GraphQLString)
          .name("bytes")
          .description(
              "Scalar for proto type bytes."
                  + " May contain any arbitrary sequence of bytes no longer than 2^32.")
          .build();
}

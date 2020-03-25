package com.google.api.graphql.rejoiner;

import com.google.protobuf.ByteString;
import com.google.protobuf.StringValue;
import graphql.Scalars;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
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
          .name("UInt32")
          .description("Scalar for proto type uint32. Uses variable-length encoding.")
          .build();

  public static final GraphQLScalarType UINT_64 =
      GraphQLScalarType.newScalar(Scalars.GraphQLLong)
          .name("UInt64")
          .description("Scalar for proto type uint64. Uses variable-length encoding.")
          .build();

  public static final GraphQLScalarType SINT_32 =
      GraphQLScalarType.newScalar(Scalars.GraphQLInt)
          .name("SInt32")
          .description(
              "Scalar for proto type sint32. Uses variable-length encoding."
                  + " Signed int value. These more efficiently encode negative numbers than regular int32s.")
          .build();

  public static final GraphQLScalarType SINT_64 =
      GraphQLScalarType.newScalar(Scalars.GraphQLLong)
          .name("SInt64")
          .description(
              "Scalar for proto type sint64. Uses variable-length encoding. Signed int value."
                  + " These more efficiently encode negative numbers than regular int64s.")
          .build();

  public static final GraphQLScalarType FIXED_32 =
      GraphQLScalarType.newScalar(Scalars.GraphQLLong)
          .name("Fixed32")
          .description(
              "Scalar for proto type fixed32. Always four bytes."
                  + " More efficient than uint32 if values are often greater than 2^28.")
          .build();

  public static final GraphQLScalarType FIXED_64 =
      GraphQLScalarType.newScalar(Scalars.GraphQLLong)
          .name("Fixed64")
          .description(
              "Scalar for proto type fixed64. Always eight bytes."
                  + " More efficient than uint64 if values are often greater than 2^56.")
          .build();

  public static final GraphQLScalarType S_FIXED_32 =
      GraphQLScalarType.newScalar(Scalars.GraphQLInt)
          .name("SFixed32")
          .description("Scalar for proto type sfixed32. Always four bytes.")
          .build();

  public static final GraphQLScalarType S_FIXED_64 =
      GraphQLScalarType.newScalar(Scalars.GraphQLLong)
          .name("SFixed64")
          .description("Scalar for proto type sfixed64. Always eight bytes.")
          .build();

  public static final GraphQLScalarType BYTES =
      GraphQLScalarType.newScalar()
          .coercing(
              new Coercing<ByteString, String>() {
                @Override
                public String serialize(Object dataFetcherResult)
                    throws CoercingSerializeException {
                  if (dataFetcherResult instanceof ByteString) {
                    return ((ByteString) dataFetcherResult).toStringUtf8();
                  } else {
                    throw new CoercingSerializeException(
                        "Invalid value '" + dataFetcherResult + "' for Bytes");
                  }
                }

                @Override
                public ByteString parseValue(Object input) throws CoercingParseValueException {
                  if (input instanceof String) {
                    ByteString result = ByteString.copyFromUtf8((String) input);
                    if (result == null) {
                      throw new CoercingParseValueException(
                          "Invalid value '" + input + "' for Bytes");
                    }
                    return result;
                  }
                  throw new CoercingParseValueException("Invalid value '" + input + "' for Bytes");
                }

                @Override
                public ByteString parseLiteral(Object input) throws CoercingParseLiteralException {
                  if (input instanceof StringValue) {
                    return ((StringValue) input).getValueBytes();
                  }
                  return null;
                }
              })
          .name("Bytes")
          .description(
              "Scalar for proto type bytes."
                  + " May contain any arbitrary sequence of bytes no longer than 2^32.")
          .build();
}

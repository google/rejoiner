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

package com.google.api.graphql.grpc;

import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.Map;

/** Converts a QueryResponse json map into a protobuf {@link Struct} object. */
public final class QueryResponseToProtoJson {

  /** Converts a json map into a protobuf {@link Struct} object. */
  public static Struct jsonToStruct(Map<String, Object> json) {
    return jsonToStructBuilder(json).build();
  }

  /** Converts a json map into a protobuf {@link Struct} builder object. */
  private static Struct.Builder jsonToStructBuilder(Map<String, Object> json) {
    Struct.Builder builder = Struct.newBuilder();
    for (Map.Entry<String, Object> entry : json.entrySet()) {
      Value structValue = value(entry.getValue());
      builder.putFields(entry.getKey(), structValue);
    }
    return builder;
  }

  /** Converts a value into a protobuf {@link Value} object. */
  private static Value value(Object value) {
    if (value instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) value;
      return Value.newBuilder().setStructValue(jsonToStruct(map)).build();
    } else if (value instanceof Value) {
      return (Value) value;
    } else if (value instanceof String) {
      return Value.newBuilder().setStringValue((String) value).build();
    } else if (value instanceof Boolean) {
      return Value.newBuilder().setBoolValue((Boolean) value).build();
    } else if (value instanceof Number) {
      return Value.newBuilder().setNumberValue(((Number) value).doubleValue()).build();
    } else if (value instanceof Iterable<?>) {
      return listValue((Iterable<?>) value);
    } else if (value instanceof Struct) {
      return Value.newBuilder().setStructValue((Struct) value).build();
    } else if (value == null) {
      return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
    } else {
      throw new IllegalArgumentException("Cannot convert " + value + " to a protobuf `Value`");
    }
  }

  /** Returns a {@link Value} representing a list of items. */
  private static Value listValue(Iterable<?> value) {
    ListValue.Builder listValue = ListValue.newBuilder();
    for (Object item : value) {
      listValue.addValues(value(item));
    }
    return Value.newBuilder().setListValue(listValue.build()).build();
  }
}

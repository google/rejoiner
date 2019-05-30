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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/** Utility class for converting structs. */
public final class StructUtil {
  private StructUtil() {}

  /** Convert a Struct to an ImmutableMap. */
  public static ImmutableMap<String, Object> toMap(Struct struct) {
    return struct
        .getFieldsMap()
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                entry -> entry.getKey(),
                entry -> {
                  Value value = entry.getValue();
                  switch (value.getKindCase()) {
                    case STRUCT_VALUE:
                      return toMap(value.getStructValue());
                    case LIST_VALUE:
                      return toList(value.getListValue());
                    default:
                      return getScalarValue(value);
                  }
                }));
  }

  private static Object getScalarValue(Value value) {
    switch (value.getKindCase()) {
      case STRUCT_VALUE:
      case LIST_VALUE:
        throw new AssertionError("value should be scalar");
      case BOOL_VALUE:
        return value.getBoolValue();
      case NUMBER_VALUE:
        // Note: this assumes all numbers are doubles. Downstream code that have access to the
        // schema can convert this number to the correct number type.
        return value.getNumberValue();
      case STRING_VALUE:
        return value.getStringValue();
      default:
        break;
    }
    return value;
  }

  private static ImmutableList<Object> toList(ListValue listValue) {
    return listValue
        .getValuesList()
        .stream()
        .map(
            value ->
                Value.KindCase.STRUCT_VALUE.equals(value.getKindCase())
                    ? toMap(value.getStructValue())
                    : getScalarValue(value))
        .collect(ImmutableList.toImmutableList());
  }
}

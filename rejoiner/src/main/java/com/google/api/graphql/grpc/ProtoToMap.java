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
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.List;
import java.util.Map;

/** Creates a Map based on a Message, while maintaining the field name case. */
public final class ProtoToMap {

  public static Map<String, Object> messageToMap(Message message) {
    ImmutableMap.Builder<String, Object> variablesBuilder = new ImmutableMap.Builder<>();
    message
        .getAllFields()
        .forEach((field, value) -> variablesBuilder.put(field.getName(), mapValues(field, value)));
    return variablesBuilder.build();
  }

  private static Object mapValues(FieldDescriptor field, Object maybeValues) {
    if (field.isRepeated()) {
      List<?> values = (List<?>) maybeValues;
      return values
          .stream()
          .map(value -> mapValue(field, value))
          .collect(ImmutableList.toImmutableList());
    }
    return mapValue(field, maybeValues);
  }

  private static Object mapValue(FieldDescriptor field, Object value) {
    if (value instanceof Message) {
      @SuppressWarnings("unchecked")
      Message message = (Message) value;
      return messageToMap(message);
    } else if (field.isMapField()) {
      // TODO: add support and test for maps
      return value;
    }
    // TODO: add support and tests for enums

    return value;
  }
}

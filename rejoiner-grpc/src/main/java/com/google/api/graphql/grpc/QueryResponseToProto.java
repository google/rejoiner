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

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import java.util.List;
import java.util.Map;

/** Fills proto with query response data. */
public final class QueryResponseToProto {

  private QueryResponseToProto() {}

  public static <T extends Message> T buildMessage(T message, Map<String, Object> fields) {
    @SuppressWarnings("unchecked")
    T populatedMessage = (T) buildMessage(message.toBuilder(), fields);
    return populatedMessage;
  }

  @SuppressWarnings("unchecked")
  private static Object buildMessage(Builder builder, Map<String, Object> fields) {
    if (fields == null) {
      return builder.build();
    }
    Descriptor descriptor = builder.getDescriptorForType();
    for (Map.Entry<String, Object> entry : fields.entrySet()) {
      if (entry.getValue() == null) {
        continue;
      }
      FieldDescriptor field = getField(descriptor, entry.getKey());
      if (entry.getValue() instanceof List<?>) {
        List<Object> values = (List<Object>) entry.getValue();
        for (Object value : values) {
          builder.addRepeatedField(field, buildValue(builder, field, value));
        }

      } else {
        builder.setField(field, buildValue(builder, field, entry.getValue()));
      }
    }
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private static Object buildValue(
      Message.Builder parentBuilder, FieldDescriptor field, Object value) {
    if (field == null) {
      return value;
    }
    if (field.getType() == FieldDescriptor.Type.MESSAGE) {
      if (field.isRepeated()) {}
      Message.Builder fieldBuilder = parentBuilder.newBuilderForField(field);
      return buildMessage(fieldBuilder, (Map<String, Object>) value);
    } else if (field.getType() == FieldDescriptor.Type.ENUM) {
      return field.getEnumType().findValueByName((String) value);
    } else {
      switch (field.getType()) {
        case FLOAT: // float is a special case
          return Float.valueOf(value.toString());
        default:
          return value;
      }
    }
  }

  private static FieldDescriptor getField(Descriptor descriptor, String name) {
    return descriptor.findFieldByName(CAMEL_TO_UNDERSCORE.convert(name));
  }

  private static final Converter<String, String> CAMEL_TO_UNDERSCORE =
      CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE);
}

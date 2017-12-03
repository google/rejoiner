package com.google.api.graphql.rejoiner;

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
    return descriptor.findFieldByName(name);
  }
}

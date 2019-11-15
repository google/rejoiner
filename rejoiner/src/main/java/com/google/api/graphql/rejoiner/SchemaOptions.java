package com.google.api.graphql.rejoiner;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

@AutoValue
public abstract class SchemaOptions {
  public static SchemaOptions defaultOptions() {
    return SchemaOptions.builder().useProtoScalarTypes(false).build();
  }

  public static SchemaOptions.Builder builder() {
    return new AutoValue_SchemaOptions.Builder().useProtoScalarTypes(false);
  }

  public abstract boolean useProtoScalarTypes();

  public abstract ImmutableMap<String, String> commentsMap();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder useProtoScalarTypes(boolean useProtoScalarTypes);

    public abstract ImmutableMap.Builder<String, String> commentsMapBuilder();

    public abstract SchemaOptions build();
  }
}

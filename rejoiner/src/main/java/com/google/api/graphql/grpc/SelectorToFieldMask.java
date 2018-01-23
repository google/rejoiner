package com.google.api.graphql.grpc;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.FieldMask;
import com.google.protobuf.FieldMask.Builder;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.Selection;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;

/** Creates a {@link FieldMask} based on a GraphQL {@link Selection}. */
public final class SelectorToFieldMask {

  private SelectorToFieldMask() {}

  private static final Converter<String, String> FIELD_TO_PROTO =
      CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE);

  public static Builder getFieldMaskForProto(
      DataFetchingEnvironment environment,
      ImmutableMap<String, FragmentDefinition> fragmentsByName,
      Descriptor descriptor,
      String startAtFieldName) {

    Builder maskFromSelectionBuilder = FieldMask.newBuilder();

    for (Field field : environment.getFields()) {
      for (Selection selection1 : field.getSelectionSet().getSelections()) {
        if (selection1 instanceof Field) {
          Field field2 = (Field) selection1;
          if (field2.getName().equals(startAtFieldName)) {
            for (Selection selection : field2.getSelectionSet().getSelections()) {
              maskFromSelectionBuilder.addAllPaths(
                  getPathsForProto("", selection, descriptor, fragmentsByName));
            }
          }
        }
      }
    }
    return maskFromSelectionBuilder;
  }

  public static Builder getFieldMaskForProto(
      DataFetchingEnvironment environment,
      ImmutableMap<String, FragmentDefinition> fragmentsByName,
      Descriptor descriptor) {

    Builder maskFromSelectionBuilder = FieldMask.newBuilder();
    for (Field field : environment.getFields()) {
      for (Selection selection : field.getSelectionSet().getSelections()) {
        maskFromSelectionBuilder.addAllPaths(
            getPathsForProto("", selection, descriptor, fragmentsByName));
      }
    }
    return maskFromSelectionBuilder;
  }

  private static ImmutableSet<String> getPathsForProto(
      String prefix,
      Selection node,
      Descriptor descriptor,
      Map<String, FragmentDefinition> fragmentsByName) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    if (node instanceof Field) {
      Field field = ((Field) node);
      String name = FIELD_TO_PROTO.convert(field.getName());
      if (descriptor.findFieldByName(name) == null) {
        if (!prefix.isEmpty()) {
          builder.add(prefix + "*");
        }
        return builder.build();
      }

      if (field.getSelectionSet() != null) {
        for (Selection selection : field.getSelectionSet().getSelections()) {
          builder.addAll(
              getPathsForProto(
                  prefix + name + ".",
                  selection,
                  descriptor.findFieldByName(name).getMessageType(),
                  fragmentsByName));
        }
      } else {
        builder.add(prefix + name);
      }
    } else if (node instanceof FragmentSpread) {
      FragmentSpread fragmentSpread = (FragmentSpread) node;
      String name = fragmentSpread.getName();
      FragmentDefinition field = fragmentsByName.get(fragmentSpread.getName());
      if (field.getSelectionSet() != null) {
        for (Selection selection : field.getSelectionSet().getSelections()) {
          builder.addAll(getPathsForProto(prefix, selection, descriptor, fragmentsByName));
        }
      } else {
        builder.add(prefix + name);
      }
    } else {
      // "not a Field"
    }
    return builder.build();
  }
}

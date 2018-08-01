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

import static graphql.schema.GraphQLObjectType.newObject;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors.GenericDescriptor;
import graphql.AssertException;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

/** Modifies a GraphQL schema by adding, removing, and replacing fields on a type. */
public final class Type {

  private Type() {}

  /** Finds a GraphQL type by it's reference name. */
  public static ModifiableType find(String typeReferenceName) {
    return new ModifiableType(typeReferenceName);
  }

  /** Finds the GraphQL type for a Proto descriptor. */
  public static ModifiableType find(GenericDescriptor typeDescriptor) {
    return new ModifiableType(ProtoToGql.getReferenceName(typeDescriptor));
  }

  /** A GraphQL type that can be modified. */
  public static final class ModifiableType {

    private final String typeName;

    private ModifiableType(String typeName) {
      this.typeName = typeName;
    }

    /** Returns a TypeModification that will add the supplied fields. */
    public TypeModification addFields(GraphQLFieldDefinition... fields) {
      return new AddFields(typeName, ImmutableList.copyOf(fields));
    }

    /** Returns a TypeModification that will add the supplied field. */
    public TypeModification addField(GraphQLFieldDefinition field) {
      return new AddField(typeName, field);
    }

    /** Returns a TypeModification that will remove the specified fields. */
    public TypeModification removeFields(String... fieldNames) {
      return new RemoveFieldsByName(typeName, ImmutableList.copyOf(fieldNames));
    }

    /** Returns a TypeModification that will remove the specified fields. */
    public TypeModification removeField(String fieldName) {
      return new RemoveFieldByName(typeName, fieldName);
    }

    /** Returns a TypeModification that will replace the specified field. */
    public TypeModification replaceField(GraphQLFieldDefinition field) {
      return new ReplaceField(typeName, field);
    }
  }

  /** A Function that modifies a GraphQLObjectType. */
  public abstract static class AbstractTypeModification implements TypeModification {

    private final String typeName;

    private AbstractTypeModification(String typeName) {
      this.typeName = typeName;
    }

    @Override
    public String getTypeName() {
      return typeName;
    }

    GraphQLObjectType.Builder toBuilder(GraphQLObjectType input) {
      return newObject()
          .name(input.getName())
          .description(input.getDescription())
          .fields(input.getFieldDefinitions());
    }
  }

  private static class AddFields extends AbstractTypeModification {

    private final ImmutableList<GraphQLFieldDefinition> fields;

    AddFields(String typeName, ImmutableList<GraphQLFieldDefinition> fields) {
      super(typeName);
      this.fields = fields;
    }

    @Override
    public GraphQLObjectType apply(GraphQLObjectType input) {
      return toBuilder(input).fields(fields).build();
    }
  }

  private static class AddField extends AbstractTypeModification {

    private final GraphQLFieldDefinition field;

    AddField(String typeName, GraphQLFieldDefinition field) {
      super(typeName);
      this.field = field;
    }

    @Override
    public GraphQLObjectType apply(GraphQLObjectType input) {
      if (input.getFieldDefinition(field.getName()) != null) {
        throw new AssertException(
            String.format("Field already added with name %s", field.getName()));
      }
      return toBuilder(input).field(field).build();
    }
  }

  private static class RemoveFieldsByName extends AbstractTypeModification {

    private final ImmutableList<String> fieldNamesToRemove;

    RemoveFieldsByName(String typeName, ImmutableList<String> fieldNamesToRemove) {
      super(typeName);
      this.fieldNamesToRemove = fieldNamesToRemove;
    }

    @Override
    public GraphQLObjectType apply(GraphQLObjectType input) {
      ImmutableList.Builder<GraphQLFieldDefinition> remainingFields = ImmutableList.builder();
      for (GraphQLFieldDefinition field : input.getFieldDefinitions()) {
        if (!fieldNamesToRemove.contains(field.getName())) {
          remainingFields.add(field);
        }
      }
      return newObject()
          .name(input.getName())
          .description(input.getDescription())
          .fields(remainingFields.build())
          .build();
    }
  }

  private static class ReplaceField extends AbstractTypeModification {
    private final GraphQLFieldDefinition field;

    ReplaceField(String typeName, GraphQLFieldDefinition field) {
      super(typeName);
      this.field = field;
    }

    @Override
    public GraphQLObjectType apply(GraphQLObjectType input) {
      return toBuilder(new RemoveFieldByName(getTypeName(), field.getName()).apply(input))
          .field(field)
          .build();
    }
  }

  private static class RemoveFieldByName extends AbstractTypeModification {

    private final String fieldNameToRemove;

    RemoveFieldByName(String typeName, String fieldNameToRemove) {
      super(typeName);
      this.fieldNameToRemove = fieldNameToRemove;
    }

    @Override
    public GraphQLObjectType apply(GraphQLObjectType input) {
      ImmutableList.Builder<GraphQLFieldDefinition> remainingFields = ImmutableList.builder();
      for (GraphQLFieldDefinition field : input.getFieldDefinitions()) {
        if (!fieldNameToRemove.equals(field.getName())) {
          remainingFields.add(field);
        }
      }
      return newObject()
          .name(input.getName())
          .description(input.getDescription())
          .fields(remainingFields.build())
          .build();
    }
  }
}

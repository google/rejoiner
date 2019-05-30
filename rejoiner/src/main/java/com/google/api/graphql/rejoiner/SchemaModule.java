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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLTypeReference;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Module for registering parts of a {@link graphql.schema.GraphQLSchema}.
 *
 * <p>Any public fields of type {@link GraphQLFieldDefinition} annotated with {@link Query} or
 * {@link Mutation} will be added to the top level query or mutation. Fields of type {@link
 * TypeModification} annotated with {@link SchemaModification} will be applied to the generated
 * schema in order to add, remove, or replace fields on a GraphQL type. Fields of type {@link
 * FileDescriptor} annotated with {@link ExtraType} will be available to GraphQL when creating the
 * final schema.
 */
public abstract class SchemaModule extends AbstractModule {

  private final Object schemaDefinition;

  /** Uses the fields and methods on the given schema definition. */
  public SchemaModule(Object schemaDefinition) {
    this.schemaDefinition = schemaDefinition;
  }

  /** Uses the fields and methods on the module itself. */
  public SchemaModule() {
    schemaDefinition = this;
  }

  private final SchemaDefinitionReader definition =
      new SchemaDefinitionReader() {
        @Override
        protected ImmutableList<GraphQLFieldDefinition> extraMutations() {
          return SchemaModule.this.extraMutations();
        }

        @Override
        protected Function<DataFetchingEnvironment, Object> handleParameter(
            Method method, int parameterIndex) {
          Annotation[] annotations = method.getParameterAnnotations()[parameterIndex];
          Annotation qualifier = null;
          for (Annotation annotation : annotations) {
            if (com.google.inject.internal.Annotations.isBindingAnnotation(
                annotation.annotationType())) {
              qualifier = annotation;
            }
          }
          final java.lang.reflect.Type[] genericParameterTypes = method.getGenericParameterTypes();
          Key<?> key =
              qualifier == null
                  ? Key.get(genericParameterTypes[parameterIndex])
                  : Key.get(genericParameterTypes[parameterIndex], qualifier);

          final com.google.inject.Provider<?> provider = binder().getProvider(key);
          return (ignored) -> provider;
        }
      };

  /**
   * Returns a reference to the GraphQL type corresponding to the supplied proto.
   *
   * <p>All types in the proto are made available to be included in the GraphQL schema.
   */
  protected final GraphQLTypeReference getTypeReference(Descriptor descriptor) {
    return definition.getTypeReference(descriptor);
  }

  protected final GraphQLTypeReference getInputTypeReference(Descriptor descriptor) {
    return definition.getInputTypeReference(descriptor);
  }

  protected ImmutableList<GraphQLFieldDefinition> extraMutations() {
    return ImmutableList.of();
  }

  protected void addQuery(GraphQLFieldDefinition query) {
    definition.addQuery(query);
  }

  protected void addMutation(GraphQLFieldDefinition mutation) {
    definition.addMutation(mutation);
  }

  protected void addQueryList(List<GraphQLFieldDefinition> queries) {
    definition.addQueryList(queries);
  }

  protected void addMutationList(List<GraphQLFieldDefinition> mutations) {
    definition.addMutationList(mutations);
  }

  protected void configureSchema() {}

  @Override
  protected final void configure() {
    Multibinder<SchemaBundle> schemaBundleProviders =
        Multibinder.newSetBinder(
            binder(), new TypeLiteral<SchemaBundle>() {}, Annotations.SchemaBundles.class);

    schemaBundleProviders
        .addBinding()
        .toProvider(
            () -> {
              configureSchema();
              return definition.createBundle(schemaDefinition);
            });

    requestInjection(this);
  }

  void addExtraType(Descriptors.Descriptor descriptor) {
    definition.addExtraType(descriptor);
  }
}

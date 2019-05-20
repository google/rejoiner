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

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Message;
import graphql.Scalars;
import graphql.schema.*;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that inspects fields and methods on a "schema definition" object.
 * This results in a {@link SchemaBundle} which can then be turned into a schema.
 *
 * <p>Any public fields of type {@link GraphQLFieldDefinition} annotated with {@link Query} or
 * {@link Mutation} will be added to the top level query or mutation. Fields of type {@link
 * TypeModification} annotated with {@link SchemaModification} will be applied to the generated
 * schema in order to add, remove, or replace fields on a GraphQL type. Fields of type {@link
 * FileDescriptor} annotated with {@link ExtraType} will be available to GraphQL when creating the
 * final schema.
 */
public class SchemaDefinitionReader {

  private final ImmutableSet.Builder<Descriptor> referencedDescriptors = ImmutableSet.builder();
  private final List<GraphQLFieldDefinition> allQueriesInModule = new ArrayList<>();
  private final List<GraphQLFieldDefinition> allMutationsInModule = new ArrayList<>();

  /**
   * Returns a reference to the GraphQL type corresponding to the supplied proto.
   *
   * <p>All types in the proto are made available to be included in the GraphQL schema.
   */
  protected final GraphQLTypeReference getTypeReference(Descriptor descriptor) {
    referencedDescriptors.add(descriptor);
    return ProtoToGql.getReference(descriptor);
  }

  protected final GraphQLTypeReference getInputTypeReference(Descriptor descriptor) {
    referencedDescriptors.add(descriptor);
    return new GraphQLTypeReference(GqlInputConverter.getReferenceName(descriptor));
  }

  protected ImmutableList<GraphQLFieldDefinition> extraMutations() {
    return ImmutableList.of();
  }

  protected void addQuery(GraphQLFieldDefinition query) {
    allQueriesInModule.add(query);
  }

  protected void addMutation(GraphQLFieldDefinition mutation) {
    allMutationsInModule.add(mutation);
  }

  protected void addQueryList(List<GraphQLFieldDefinition> queries) {
    allQueriesInModule.addAll(queries);
  }

  protected void addMutationList(List<GraphQLFieldDefinition> mutations) {
    allMutationsInModule.addAll(mutations);
  }

  public SchemaBundle createBundle(final Object schemaDefinition) {
    final Class<?> moduleClass = schemaDefinition.getClass();

    for (Method method : findMethods(moduleClass, Query.class)) {
      Query query = method.getAnnotationsByType(Query.class)[0];
      allQueriesInModule.add(
              methodToFieldDefinition(schemaDefinition, method, query.value(), query.fullName(), null));
    }
    for (Method method : findMethods(moduleClass, Mutation.class)) {
      Mutation mutation = method.getAnnotationsByType(Mutation.class)[0];
      allMutationsInModule.add(
              methodToFieldDefinition(schemaDefinition, method, mutation.value(), mutation.fullName(), null));
    }

    final List<NodeDataFetcher> nodeDataFetchers = new ArrayList<>();
    final List<TypeModification> schemaModifications = new ArrayList<>();

    for (Method method : findMethods(moduleClass, RelayNode.class)) {
      GraphQLFieldDefinition graphQLFieldDefinition =
              methodToFieldDefinition(schemaDefinition, method, "_NOT_USED_", "_NOT_USED_", null);
      nodeDataFetchers.add(
              new NodeDataFetcher(graphQLFieldDefinition.getType().getName()) {
                @Override
                public Object apply(String s) {
                  // TODO: Don't hardcode the arguments structure.
                  try {
                    return null;
                    //                      return graphQLFieldDefinition
                    //                              .getDataFetcher()
                    //                              .get(
                    //
                    // DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                    //
                    // .arguments(ImmutableMap.of("input", ImmutableMap.of("id",
                    // s)))
                    //                                  .build());
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                }
              });
    }
    try {
      for (Method method : findMethods(moduleClass, SchemaModification.class)) {
        SchemaModification annotation = method.getAnnotationsByType(SchemaModification.class)[0];
        String name = annotation.addField();
        Class<?> typeClass = annotation.onType();
        Descriptor typeDescriptor = (Descriptor) typeClass.getMethod("getDescriptor").invoke(null);
        referencedDescriptors.add(typeDescriptor);
        schemaModifications.add(methodToTypeModification(schemaDefinition, method, name, typeDescriptor));
      }
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }

    SchemaBundle.Builder schemaBundleBuilder = SchemaBundle.builder();

    allMutationsInModule.addAll(extraMutations());

    try {
      for (Field field : findQueryFields(moduleClass)) {
        field.setAccessible(true);
        allQueriesInModule.add((GraphQLFieldDefinition) field.get(schemaDefinition));
      }

      for (Field field : findMutationFields(moduleClass)) {
        field.setAccessible(true);
        allMutationsInModule.add((GraphQLFieldDefinition) field.get(schemaDefinition));
      }

      for (Field field : findTypeModificationFields(moduleClass)) {
        field.setAccessible(true);
        schemaBundleBuilder
                .modificationsBuilder()
                .add((TypeModification) field.get(schemaDefinition));
      }

      for (Field field : findExtraTypeFields(moduleClass)) {
        field.setAccessible(true);
        schemaBundleBuilder
                .fileDescriptorsBuilder()
                .add((FileDescriptor) field.get(schemaDefinition));
      }

      Namespace namespaceAnnotation = findClassAnnotation(moduleClass, Namespace.class);

      if (namespaceAnnotation == null) {
        schemaBundleBuilder.mutationFieldsBuilder().addAll(allMutationsInModule);
        schemaBundleBuilder.queryFieldsBuilder().addAll(allQueriesInModule);
      } else {
        String namespace = namespaceAnnotation.value();
        if (!allQueriesInModule.isEmpty()) {
          schemaBundleBuilder
                  .queryFieldsBuilder()
                  .add(
                          GraphQLFieldDefinition.newFieldDefinition()
                                  .staticValue("")
                                  .name(namespace)
                                  .description(namespace)
                                  .type(
                                          GraphQLObjectType.newObject()
                                                  .name("_QUERY_FIELD_GROUP_" + namespace)
                                                  .fields(allQueriesInModule)
                                                  .build())
                                  .build());
        }
        if (!allMutationsInModule.isEmpty()) {
          schemaBundleBuilder
                  .mutationFieldsBuilder()
                  .add(
                          GraphQLFieldDefinition.newFieldDefinition()
                                  .staticValue("")
                                  .name(namespace)
                                  .description(namespace)
                                  .type(
                                          GraphQLObjectType.newObject()
                                                  .name("_MUTATION_FIELD_GROUP_" + namespace)
                                                  .fields(allMutationsInModule)
                                                  .build())
                                  .build());
        }
      }

      schemaBundleBuilder.nodeDataFetchersBuilder().addAll(nodeDataFetchers);
      schemaBundleBuilder.modificationsBuilder().addAll(schemaModifications);

    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    referencedDescriptors
            .build()
            .forEach(
                    descriptor ->
                            schemaBundleBuilder.fileDescriptorsBuilder().add(descriptor.getFile()));

    return schemaBundleBuilder.build();
  }

  void addExtraType(Descriptor descriptor) {
    referencedDescriptors.add(descriptor);
  }

  /**
   * Returns an {@link ImmutableSet} of all the methods in {@code moduleClass} or its super classes
   * that are annotated with {@link Query}.
   *
   * @param moduleClass
   */
  private static ImmutableSet<Field> findTypeModificationFields(
          Class<?> moduleClass) {
    return findFields(moduleClass, SchemaModification.class, TypeModification.class);
  }

  /**
   * Returns an {@link ImmutableSet} of all the methods in {@code moduleClass} or its super classes
   * that are annotated with {@link Query}.
   *
   * @param moduleClass
   */
  private static ImmutableSet<Field> findExtraTypeFields(
          Class<?> moduleClass) {
    return findFields(moduleClass, ExtraType.class, FileDescriptor.class);
  }

  /**
   * Returns an {@link ImmutableSet} of all the methods in {@code moduleClass} or its super classes
   * that are annotated with {@link Query}.
   *
   * @param moduleClass
   */
  private static ImmutableSet<Field> findMutationFields(Class<?> moduleClass) {
    return findFields(moduleClass, Mutation.class, GraphQLFieldDefinition.class);
  }

  /**
   * Returns an {@link ImmutableSet} of all the methods in {@code moduleClass} or its super classes
   * that are annotated with {@link Query}.
   *
   * @param moduleClass
   */
  private static ImmutableSet<Field> findQueryFields(Class<?> moduleClass) {
    return findFields(moduleClass, Query.class, GraphQLFieldDefinition.class);
  }

  /**
   * Returns an {@link ImmutableSet} of all the methods in {@code moduleClass} or its super classes
   * that have the expected type and annotation.
   */
  private static ImmutableSet<Method> findMethods(
          Class<?> moduleClass, Class<? extends Annotation> targetAnnotation) {
    Class<?> clazz = moduleClass;
    ImmutableSet.Builder<Method> nodesBuilder = ImmutableSet.builder();
    while (clazz != null && !SchemaDefinitionReader.class.equals(clazz)) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.isAnnotationPresent(targetAnnotation)) {
          nodesBuilder.add(method);
        }
      }
      clazz = clazz.getSuperclass();
    }
    return nodesBuilder.build();
  }

  /**
   * Returns an {@link ImmutableSet} of all the methods in {@code moduleClass} or its super classes
   * that have the expected type and annotation.
   */
  private static <T extends Annotation> T findClassAnnotation(
          Class<?> moduleClass, Class<T> targetAnnotation) {
    Class<?> clazz = moduleClass;
    while (clazz != null && !SchemaDefinitionReader.class.equals(clazz)) {
      T annotation = clazz.getAnnotation(targetAnnotation);
      if (annotation != null) {
        return annotation;
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  /**
   * Returns an {@link ImmutableSet} of all the fields in {@code moduleClass} or its super classes
   * that have the expected type and annotation.
   */
  private static ImmutableSet<Field> findFields(
          Class<?> moduleClass,
          Class<? extends Annotation> targetAnnotation,
          Class<?> expectedType) {
    Class<?> clazz = moduleClass;
    ImmutableSet.Builder<Field> nodesBuilder = ImmutableSet.builder();
    while (clazz != null && !SchemaDefinitionReader.class.equals(clazz)) {
      for (Field method : clazz.getDeclaredFields()) {
        if (method.isAnnotationPresent(targetAnnotation)) {
          Preconditions.checkArgument(
                  method.getType() == expectedType,
                  "Field %s should be type %s",
                  method,
                  expectedType.getSimpleName());
          nodesBuilder.add(method);
        }
      }
      clazz = clazz.getSuperclass();
    }
    return nodesBuilder.build();
  }

  /**
   * Override to implement additional parametertypes
   */
  protected Function<DataFetchingEnvironment, Object> handleParameter(Method method, int parameterIndex) {
    return null;
  }

  @AutoValue
  abstract static class MethodMetadata {
    @Nullable
    abstract Function<DataFetchingEnvironment, ?> function();

    @Nullable
    abstract GraphQLArgument argument();

    static MethodMetadata create(
            Function<DataFetchingEnvironment, ?> value, GraphQLArgument argument) {
      return new AutoValue_SchemaDefinition_MethodMetadata(value, argument);
    }

    static MethodMetadata create(Function<DataFetchingEnvironment, ?> value) {
      return new AutoValue_SchemaDefinition_MethodMetadata(value, null);
    }

    Object getParameterValue(DataFetchingEnvironment environment) {
      return function().apply(environment);
    }

    boolean hasArgument() {
      return argument() != null;
    }

  }

  private TypeModification methodToTypeModification(
          Object module, Method method, String name, Descriptor typeDescriptor) {
    GraphQLFieldDefinition fieldDef = methodToFieldDefinition(module, method, name, name, typeDescriptor);
    return Type.find(typeDescriptor).addField(fieldDef);
  }

  private GraphQLFieldDefinition methodToFieldDefinition(
          Object module, Method method, String name, @Nullable String fullName, @Nullable Descriptor descriptor) {
    method.setAccessible(true);
    try {
      ImmutableList<MethodMetadata> methodParameters = getMethodMetadata(method, descriptor);
      DataFetcher dataFetcher =
              (DataFetchingEnvironment environment) -> {
                Object[] methodParameterValues = new Object[methodParameters.size()];
                for (int i = 0; i < methodParameters.size(); i++) {
                  methodParameterValues[i] = methodParameters.get(i).getParameterValue(environment);
                }
                try {
                  return method.invoke(module, methodParameterValues);
                } catch (InvocationTargetException e) {
                  Throwable cause = e.getCause();
                  if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                  }
                  throw new RuntimeException(cause);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              };

      GraphQLOutputType returnType = getReturnType(method);

      // Create GraphQL Field Definition
      GraphQLFieldDefinition.Builder fieldDef = GraphQLFieldDefinition.newFieldDefinition();
      fieldDef.type(returnType);
      fieldDef.name(name);
      fieldDef.description(DescriptorSet.COMMENTS.get(fullName));
      for (MethodMetadata methodMetadata : methodParameters) {
        if (methodMetadata.hasArgument()) {
          fieldDef.argument(methodMetadata.argument());
        }
      }
      fieldDef.dataFetcher(dataFetcher);
      return fieldDef.build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  ImmutableMap<java.lang.reflect.Type, GraphQLScalarType> javaTypeToScalarMap =
          ImmutableMap.of(
                  String.class, Scalars.GraphQLString,
                  Integer.class, Scalars.GraphQLInt,
                  Boolean.class, Scalars.GraphQLBoolean,
                  Long.class, Scalars.GraphQLLong,
                  Float.class, Scalars.GraphQLFloat);

  private GraphQLOutputType getReturnType(Method method)
          throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    // Currently it's assumed the response is of type Message, ListenableFuture<? extends
    // Message>, ImmutableList<Message>, ListenableFuture<ImmutableList<? extend Message>>, oe
    // any Scalar type.

    // Assume Message or Scalar
    if (!(method.getGenericReturnType() instanceof ParameterizedType)) {
      Class<?> returnType = method.getReturnType();
      if (Message.class.isAssignableFrom(returnType)) {
        @SuppressWarnings("unchecked")
        Class<? extends Message> responseClass = (Class<? extends Message>) method.getReturnType();
        Descriptor responseDescriptor =
                (Descriptor) responseClass.getMethod("getDescriptor").invoke(null);
        referencedDescriptors.add(responseDescriptor);
        return ProtoToGql.getReference(responseDescriptor);
      }
      if (javaTypeToScalarMap.containsKey(returnType)) {
        return javaTypeToScalarMap.get(returnType);
      }
      throw new RuntimeException("Unknown scalar type: " + returnType.getTypeName());
    }

    ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
    // TODO: handle collections of Java Scalars

    // Assume ListenableFuture<ImmutableList<? extends Message>>
    java.lang.reflect.Type genericTypeValue = genericReturnType.getActualTypeArguments()[0];
    if (genericTypeValue instanceof ParameterizedType) {
      java.lang.reflect.Type listElType =
              ((ParameterizedType) genericTypeValue).getActualTypeArguments()[0];
      @SuppressWarnings("unchecked")
      Class<? extends Message> responseClass = (Class<? extends Message>) listElType;
      Descriptor responseDescriptor =
              (Descriptor) responseClass.getMethod("getDescriptor").invoke(null);
      referencedDescriptors.add(responseDescriptor);
      return new GraphQLList(new GraphQLNonNull(ProtoToGql.getReference(responseDescriptor)));
    }

    // ImmutableList<? extends Message>
    if (ImmutableList.class.isAssignableFrom((Class<?>) genericReturnType.getRawType())) {
      @SuppressWarnings("unchecked")
      Class<? extends Message> responseClass = (Class<? extends Message>) genericTypeValue;
      Descriptor responseDescriptor =
              (Descriptor) responseClass.getMethod("getDescriptor").invoke(null);
      referencedDescriptors.add(responseDescriptor);
      return new GraphQLList(new GraphQLNonNull(ProtoToGql.getReference(responseDescriptor)));
    }

    // ListenableFuture<? extends Message>
    @SuppressWarnings("unchecked")
    Class<? extends Message> responseClass =
            (Class<? extends Message>) genericReturnType.getActualTypeArguments()[0];
    Descriptor responseDescriptor =
            (Descriptor) responseClass.getMethod("getDescriptor").invoke(null);
    referencedDescriptors.add(responseDescriptor);
    return ProtoToGql.getReference(responseDescriptor);
  }

  private ImmutableList<MethodMetadata> getMethodMetadata(
          Method method, @Nullable Descriptor sourceDescriptor)
          throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    final Class<?>[] parameterTypes = method.getParameterTypes();

    ImmutableList.Builder<MethodMetadata> listBuilder = ImmutableList.builder();
    for (int i = 0; i < parameterTypes.length; i++) {

      Class<?> parameterType = parameterTypes[i];
      if (Message.class.isAssignableFrom(parameterType)) {
        @SuppressWarnings("unchecked")
        Class<? extends Message> requestClass = (Class<? extends Message>) parameterType;
        Descriptor requestDescriptor =
                (Descriptor) requestClass.getMethod("getDescriptor").invoke(null);
        if (sourceDescriptor != null
                && requestDescriptor.getFullName().equals(sourceDescriptor.getFullName())) {
          Function<DataFetchingEnvironment, ?> function = environment -> environment.getSource();
          listBuilder.add(MethodMetadata.create(function));
        } else {
          GqlInputConverter inputConverter =
                  GqlInputConverter.newBuilder().add(requestDescriptor.getFile()).build();
          addExtraType(requestDescriptor);
          Message message =
                  ((Message.Builder) requestClass.getMethod("newBuilder").invoke(null)).build();
          String argName = getArgName(method.getParameterAnnotations()[i]);
          Function<DataFetchingEnvironment, ?> function =
                  environment -> {
                    Message req =
                            inputConverter.createProtoBuf(
                                    requestDescriptor, message.toBuilder(), environment.getArgument(argName));
                    return req;
                  };
          GraphQLArgument argument = GqlInputConverter.createArgument(requestDescriptor, argName);
          listBuilder.add(MethodMetadata.create(function, argument));
        }
      } else if (isArg(method.getParameterAnnotations()[i])) {
        if (javaTypeToScalarMap.containsKey(parameterType)) {
          String argName = getArgName(method.getParameterAnnotations()[i]);
          Function<DataFetchingEnvironment, ?> function =
                  environment -> environment.getArgument(argName);
          GraphQLArgument argument =
                  GraphQLArgument.newArgument()
                          .name(argName)
                          .type(javaTypeToScalarMap.get(parameterType))
                          .build();
          listBuilder.add(MethodMetadata.create(function, argument));
        } else {
          throw new RuntimeException("Unknown arg type: " + parameterType.getName());
        }

      } else if (DataFetchingEnvironment.class.isAssignableFrom(parameterType)) {
        listBuilder.add(MethodMetadata.create(Functions.identity()));
      } else {
        listBuilder.add(MethodMetadata.create(handleParameter(method, i)));
      }
    }
    return listBuilder.build();
  }

  private static boolean isArg(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().isAssignableFrom(Arg.class)) {
        return true;
      }
    }
    return false;
  }

  private static String getArgName(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().isAssignableFrom(Arg.class)) {
        return ((Arg) annotation).value();
      }
    }
    return "input";
  }
}

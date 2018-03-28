package com.google.api.graphql.rejoiner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.stream.Stream;

/** SchemaModule that generates queries and mutations for gRPC clients. */
public abstract class GrpcSchemaModule extends SchemaModule {

  protected ImmutableList<GraphQLFieldDefinition> serviceToFields(
      Class<?> client, ImmutableList<String> methodWhitelist) {

    return getMethods(client, methodWhitelist)
        .map(
            method -> {
              try {
                method.setAccessible(true);
                ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
                GraphQLOutputType responseType = getReturnType(returnType);
                Class<? extends Message> requestMessageClass =
                    (Class<? extends Message>) method.getParameterTypes()[0];
                Descriptors.Descriptor requestDescriptor =
                    (Descriptors.Descriptor)
                        requestMessageClass.getMethod("getDescriptor").invoke(null);
                Message requestMessage =
                    ((Message.Builder) requestMessageClass.getMethod("newBuilder").invoke(null))
                        .buildPartial();
                Provider<?> service = getProvider(client);

                GqlInputConverter inputConverter =
                    GqlInputConverter.newBuilder().add(requestDescriptor.getFile()).build();

                DataFetcher dataFetcher =
                    (DataFetchingEnvironment env) -> {
                      Message input =
                          inputConverter.createProtoBuf(
                              requestDescriptor,
                              requestMessage.toBuilder(),
                              env.getArgument("input"));
                      try {
                        Object[] methodParameterValues = new Object[] {input};
                        return method.invoke(service.get(), methodParameterValues);
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      }
                    };

                return GraphQLFieldDefinition.newFieldDefinition()
                    .name(method.getName())
                    .argument(GqlInputConverter.createArgument(requestDescriptor, "input"))
                    .type(responseType)
                    .dataFetcher(dataFetcher)
                    .build();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            })
        .collect(ImmutableList.toImmutableList());
  }

  private Stream<Method> getMethods(Class<?> clientClass, ImmutableList<String> methodWhitelist) {
    ImmutableSet<String> asyncNameWhitelist =
        methodWhitelist.stream().collect(ImmutableSet.toImmutableSet());
    return ImmutableList.copyOf(clientClass.getMethods())
        .stream()
        .filter(method -> asyncNameWhitelist.contains(method.getName()));
  }

  /** Returns a GraphQLOutputType for type T for an input of ListenableFuture<T>. */
  private GraphQLOutputType getReturnType(ParameterizedType parameterizedType)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<? extends Message> responseClass =
        (Class<? extends Message>) parameterizedType.getActualTypeArguments()[0];
    Descriptors.Descriptor responseDescriptor =
        (Descriptors.Descriptor) responseClass.getMethod("getDescriptor").invoke(null);
    addExtraType(responseDescriptor);
    return ProtoToGql.getReference(responseDescriptor);
  }
}

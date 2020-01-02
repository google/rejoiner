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

/**
 * SchemaModule that generates queries and mutations for GAX gRPC clients, such as the Google Cloud
 * Platform APIs.
 */
public abstract class GaxSchemaModule extends SchemaModule {

  protected ImmutableList<GraphQLFieldDefinition> serviceToFields(
      Class<?> client, ImmutableList<String> methodWhitelist) {
    return getMethods(client, methodWhitelist)
        .map(
            methodWrapper -> {
              try {
                methodWrapper.setAccessible(true);
                /* com.google.api.gax.rpc.UnaryCallable<Req, Resp> */
                ParameterizedType callable =
                    (ParameterizedType) methodWrapper.getGenericReturnType();
                GraphQLOutputType responseType = getReturnType(callable);
                Class<? extends Message> requestMessageClass =
                    (Class<? extends Message>) callable.getActualTypeArguments()[0];
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
                        Object callableInstance = methodWrapper.invoke(service.get());
                        Method method =
                            callableInstance.getClass().getMethod("futureCall", Object.class);
                        method.setAccessible(true);
                        Object[] methodParameterValues = new Object[] {input};
                        return method.invoke(callableInstance, methodParameterValues);
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      }
                    };

                return GraphQLFieldDefinition.newFieldDefinition()
                    .name(transformName(methodWrapper.getName()))
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
        methodWhitelist
            .stream()
            .map(name -> name + "Callable")
            .collect(ImmutableSet.toImmutableSet());

    return ImmutableList.copyOf(clientClass.getMethods())
        .stream()
        .filter(method -> asyncNameWhitelist.contains(method.getName()));
  }

  private GraphQLOutputType getReturnType(ParameterizedType parameterizedType)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<? extends Message> responseClass =
        (Class<? extends Message>) parameterizedType.getActualTypeArguments()[1];
    Descriptors.Descriptor responseDescriptor =
        (Descriptors.Descriptor) responseClass.getMethod("getDescriptor").invoke(null);
    addExtraType(responseDescriptor);
    return ProtoToGql.getReference(responseDescriptor);
  }

  private static final int LENGTH_OF_CALLABLE = 8;

  private static String transformName(String name) {
    return name.substring(0, name.length() - LENGTH_OF_CALLABLE);
  }
}

package com.google.api.graphql.rejoiner;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class ProtoDataFetcher implements DataFetcher<Object> {

  private static final Converter<String, String> LOWER_CAMEL_TO_UPPER =
          CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);

  private final String name;
  private Method method = null;

  ProtoDataFetcher(String name) {
    this.name = name;
  }

  @Override
  public Object get(DataFetchingEnvironment environment) {
    Object source = environment.getSource();
    if (source == null) {
      return null;
    }
    if (source instanceof Map) {
      return ((Map<?, ?>) source).get(name);
    }

    if(method == null) {
      GraphQLType type = environment.getFieldType();
      if (type instanceof GraphQLNonNull) {
        type = ((GraphQLNonNull) type).getWrappedType();
      }
      if (type instanceof GraphQLList) {

        Object listValue = call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name) + "List");
        if (listValue != null) {
          return listValue;
        }
        Object mapValue = call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name) + "Map");
        if (mapValue == null) {
          return null;
        }
        Map<?, ?> map = (Map<?, ?>) mapValue;
        return map.entrySet().stream()
                .map(entry -> ImmutableMap.of("key", entry.getKey(), "value", entry.getValue()))
                .collect(toImmutableList());
      }
      if (type instanceof GraphQLEnumType) {
        Object o = call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name));
        if (o != null) {
          return o.toString();
        }
      }

      return call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name));

    }

    try {
      method.invoke(source);
    } catch (InvocationTargetException |IllegalAccessException e) {
      throw new RuntimeException(e);
    }

  }

  private static Object call(Object object, String methodName) {
    try {
      Method method = object.getClass().getMethod(methodName);
      return method.invoke(object);
    } catch (NoSuchMethodException e) {
      return null;
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}

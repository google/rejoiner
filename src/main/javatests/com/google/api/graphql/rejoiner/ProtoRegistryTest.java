package com.google.api.graphql.rejoiner;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import graphql.schema.GraphQLType;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.api.graphql.rejoiner.ProtoRegistry}. */
@RunWith(JUnit4.class)
public final class ProtoRegistryTest {

  @Test
  public void protoRegistryShouldIncludeAllProtoTypesFromFile() {
    Set<GraphQLType> graphQLTypes =
        ProtoRegistry.newBuilder().add(TestProto.getDescriptor()).build().listTypes();
    assertThat(FluentIterable.from(graphQLTypes).transform(GET_NAME))
        .containsExactly(
            "javatests_com_google_api_graphql_rejoiner_proto_Proto1",
            "javatests_com_google_api_graphql_rejoiner_proto_Proto2",
            "javatests_com_google_api_graphql_rejoiner_proto_Proto1_InnerProto",
            "javatests_com_google_api_graphql_rejoiner_proto_Proto2_TestEnum");
  }

  private static final Function<GraphQLType, String> GET_NAME = type -> type.getName();
}

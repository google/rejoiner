package com.google.api.graphql.rejoiner;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.graphql.rejoiner.TestProto.Proto1;
import com.google.api.graphql.rejoiner.TestProto.Proto1.InnerProto;
import com.google.api.graphql.rejoiner.TestProto.Proto2;
import com.google.api.graphql.rejoiner.TestProto.Proto2.TestEnum;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLObjectType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.api.graphql.rejoiner.ProtoToGql}. */
@RunWith(JUnit4.class)
public final class ProtoToGqlTest {

  @Test
  public void getReferenceNameShouldReturnCorrectValueForMessages() {
    assertThat(ProtoToGql.getReferenceName(Proto1.getDescriptor()))
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto1");
    assertThat(ProtoToGql.getReferenceName(Proto2.getDescriptor()))
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto2");
  }

  @Test
  public void getReferenceNameShouldReturnCorrectValueForInnerMessages() {
    assertThat(ProtoToGql.getReferenceName(InnerProto.getDescriptor()))
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto1_InnerProto");
  }

  @Test
  public void getReferenceNameShouldReturnCorrectValueForEnums() {
    assertThat(ProtoToGql.getReferenceName(TestEnum.getDescriptor()))
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto2_TestEnum");
  }

  @Test
  public void convertShouldWorkForMessage() {
    GraphQLObjectType result = ProtoToGql.convert(Proto1.getDescriptor());
    assertThat(result.getName())
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto1");
    assertThat(result.getFieldDefinitions()).hasSize(4);
  }

  @Test
  public void convertShouldWorkForEnums() {
    GraphQLEnumType result = ProtoToGql.convert(TestEnum.getDescriptor());
    assertThat(result.getName())
        .isEqualTo("javatests_com_google_api_graphql_rejoiner_proto_Proto2_TestEnum");
    assertThat(result.getValues()).hasSize(3);
    assertThat(result.getValues().stream().map(a -> a.getName()).toArray())
        .asList()
        .containsExactly("UNKNOWN", "FOO", "BAR");
  }
}

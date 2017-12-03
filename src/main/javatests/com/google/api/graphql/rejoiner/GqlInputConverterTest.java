package com.google.api.graphql.rejoiner;

import com.google.api.graphql.rejoiner.TestProto.Proto1;
import com.google.api.graphql.rejoiner.TestProto.Proto2;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Message;
import graphql.schema.GraphQLArgument;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.api.graphql.rejoiner.GqlInputConverter}. */
@RunWith(JUnit4.class)
public final class GqlInputConverterTest {

  @Test(expected = NullPointerException.class)
  public void unknownProtoShouldFail() {
    GqlInputConverter inputConverter = GqlInputConverter.newBuilder().build();
    inputConverter.createArgument(TestProto.Proto1.getDescriptor(), "input");
  }

  @Test
  public void inputConverterShouldFillProtoBuf() {
    GqlInputConverter inputConverter =
        GqlInputConverter.newBuilder().add(TestProto.getDescriptor().getFile()).build();
    Message protoBuf =
        inputConverter.createProtoBuf(
            Proto1.getDescriptor(),
            Proto1.newBuilder(),
            ImmutableMap.of(
                "id", "id", "int_field", 123, "test_proto", ImmutableMap.of("inner_id", "1")));
    ProtoTruth.assertThat(protoBuf)
        .isEqualTo(
            Proto1.newBuilder()
               .setId("id")
                .setIntField(123)
                .setTestProto(Proto2.newBuilder().setInnerId("1").build())
                .build());
  }

  @Test
  public void inputConverterShouldCreateArgument() {
    GqlInputConverter inputConverter =
        GqlInputConverter.newBuilder().add(TestProto.getDescriptor().getFile()).build();
    GraphQLArgument argument = inputConverter.createArgument(Proto1.getDescriptor(), "input");
    Truth.assertThat(argument.getName()).isEqualTo("input");
    Truth.assertThat(argument.getType().getName())
        .isEqualTo("Input_javatests_com_google_api_graphql_rejoiner_proto_Proto1");
  }

  @Test
  public void inputConverterShouldCreateArgumentForMessagesInSameFile() {
    GqlInputConverter inputConverter =
        GqlInputConverter.newBuilder().add(TestProto.getDescriptor().getFile()).build();
    GraphQLArgument argument = inputConverter.createArgument(Proto2.getDescriptor(), "input");
    Truth.assertThat(argument.getName()).isEqualTo("input");
    Truth.assertThat(argument.getType().getName())
        .isEqualTo("Input_javatests_com_google_api_graphql_rejoiner_proto_Proto2");
  }
}

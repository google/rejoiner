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

import com.google.api.graphql.rejoiner.TestProto.Proto1;
import com.google.api.graphql.rejoiner.TestProto.Proto2;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Message;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputObjectType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.api.graphql.rejoiner.GqlInputConverter}. */
@RunWith(JUnit4.class)
public final class GqlInputConverterTest {

  @Test
  public void unknownProtoShouldPass() {
    GqlInputConverter inputConverter = GqlInputConverter.newBuilder().build();
    Truth.assertThat(inputConverter.createArgument(Proto1.getDescriptor(), "input")).isNotNull();
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
                "id", "id", "intField", 123, "testProto", ImmutableMap.of("innerId", "1")));
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
  public void inputConverterShouldCreateInputTypeWithCamelCaseName() {
    GqlInputConverter inputConverter =
        GqlInputConverter.newBuilder().add(TestProto.getDescriptor().getFile()).build();
    GraphQLInputObjectType input =
        (GraphQLInputObjectType) inputConverter.getInputType(Proto1.getDescriptor());
    Truth.assertThat(input.getField("intField")).isNotNull();
    Truth.assertThat(input.getField("camelCaseName")).isNotNull();
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

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

package com.google.api.graphql.grpc;

import com.google.api.graphql.rejoiner.TestProto;
import com.google.api.graphql.rejoiner.TestProto.Proto1;
import com.google.api.graphql.rejoiner.TestProto.Proto2;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.extensions.proto.ProtoTruth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link QueryResponseToProto}. */
@RunWith(JUnit4.class)
public final class QueryResponseToProtoTest {

  @Test
  public void getReferenceNameShouldReturnCorrectValueForMessages() {
    ProtoTruth.assertThat(
            QueryResponseToProto.buildMessage(
                TestProto.Proto1.getDefaultInstance(),
                ImmutableMap.of(
                    "id",
                    "abc",
                    "int_field",
                    123,
                    "test_proto",
                    ImmutableMap.of("inner_id", "abc_inner"))))
        .isEqualTo(
            Proto1.newBuilder()
                .setId("abc")
                .setIntField(123)
                .setTestProto(Proto2.newBuilder().setInnerId("abc_inner"))
                .build());
  }
}

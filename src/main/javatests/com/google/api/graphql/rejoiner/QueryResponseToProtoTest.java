package com.google.api.graphql.rejoiner;

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

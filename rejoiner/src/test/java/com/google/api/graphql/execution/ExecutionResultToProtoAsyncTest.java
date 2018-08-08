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

package com.google.api.graphql.execution;

import com.google.api.graphql.ErrorType;
import com.google.api.graphql.GraphqlError;
import com.google.api.graphql.rejoiner.TestProto.Proto1;
import com.google.api.graphql.rejoiner.TestProto.Proto2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import com.google.common.truth.extensions.proto.ProtoTruth;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.InvalidSyntaxError;
import graphql.execution.ExecutionPath;
import graphql.language.SourceLocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/** Unit tests for {@link ExecutionResultToProtoAsync}. */
@RunWith(JUnit4.class)
public final class ExecutionResultToProtoAsyncTest {

  @Test
  public void toProtoExecutionResultShouldReturnData()
      throws ExecutionException, InterruptedException {
    CompletableFuture<ProtoExecutionResult<Proto1>> executionResultCompletableFuture =
        ExecutionResultToProtoAsync.toProtoExecutionResult(
            Proto1.getDefaultInstance(),
            CompletableFuture.completedFuture(
                ExecutionResultImpl.newExecutionResult()
                    .data(
                        ImmutableMap.of(
                            "id",
                            "abc",
                            "intField",
                            123,
                            "testProto",
                            ImmutableMap.of(
                                "innerId", "abc_inner", "enums", ImmutableList.of("FOO"))))
                    .build()));
    ProtoTruth.assertThat(executionResultCompletableFuture.get().message())
        .isEqualTo(
            Proto1.newBuilder()
                .setId("abc")
                .setIntField(123)
                .setTestProto(
                    Proto2.newBuilder()
                        .setInnerId("abc_inner")
                        .addEnumsValue(Proto2.TestEnum.FOO_VALUE))
                .build());
    Truth.assertThat(executionResultCompletableFuture.get().errors()).isEmpty();
  }

  @Test
  public void toProtoExecutionResultShouldReturnDataAndError()
      throws ExecutionException, InterruptedException {

    ExceptionWhileDataFetching exceptionWhileDataFetching =
        new ExceptionWhileDataFetching(
            ExecutionPath.rootPath(),
            new RuntimeException("hello world"),
            new SourceLocation(10, 20));
    CompletableFuture<ProtoExecutionResult<Proto1>> executionResultCompletableFuture =
        ExecutionResultToProtoAsync.toProtoExecutionResult(
            Proto1.getDefaultInstance(),
            CompletableFuture.completedFuture(
                ExecutionResultImpl.newExecutionResult()
                    .data(
                        ImmutableMap.of(
                            "id",
                            "abc",
                            "intField",
                            123,
                            "testProto",
                            ImmutableMap.of(
                                "innerId", "abc_inner", "enums", ImmutableList.of("FOO"))))
                    .addError(exceptionWhileDataFetching)
                    .build()));
    ProtoTruth.assertThat(executionResultCompletableFuture.get().message())
        .isEqualTo(
            Proto1.newBuilder()
                .setId("abc")
                .setIntField(123)
                .setTestProto(
                    Proto2.newBuilder()
                        .setInnerId("abc_inner")
                        .addEnumsValue(Proto2.TestEnum.FOO_VALUE))
                .build());
    ProtoTruth.assertThat(executionResultCompletableFuture.get().errors())
        .containsExactly(
            GraphqlError.newBuilder()
                .setMessage("Exception while fetching data () : hello world")
                .addLocations(
                    com.google.api.graphql.SourceLocation.newBuilder().setLine(10).setColumn(20))
                .setType(ErrorType.DATA_FETCHING_EXCEPTION)
                .build());
  }
}

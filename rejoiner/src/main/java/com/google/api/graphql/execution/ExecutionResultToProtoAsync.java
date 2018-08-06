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

import com.google.api.graphql.grpc.QueryResponseToProto;
import com.google.protobuf.Message;
import graphql.ExecutionResult;
import graphql.ErrorType;
import graphql.ExecutionResult;
import graphql.GraphQLError;

import java.util.concurrent.CompletableFuture;

/** Transforms ExecutionResult into generated proto Messages. */
public final class ExecutionResultToProtoAsync {

  /** Transforms an async ExecutionResult into a ProtoExecutionResult. */
  public static <T extends Message>
      CompletableFuture<ProtoExecutionResult<T>> toProtoExecutionResult(
          T message, CompletableFuture<ExecutionResult> executionResultCompletableFuture) {
    return executionResultCompletableFuture.thenApply(
        executionResult ->
            ProtoExecutionResult.create(
                QueryResponseToProto.buildMessage(message, executionResult.toSpecification())));
  }
  /**
   * Transforms an async ExecutionResult into a proto Messages.
   *
   * <p>Note that only data and not error information is represented in the Proto.
   */
  public static <T extends Message> CompletableFuture<T> toProtoMessage(
      T message, CompletableFuture<ExecutionResult> executionResultCompletableFuture) {
    return executionResultCompletableFuture.thenApply(
        executionResult ->
            QueryResponseToProto.buildMessage(message, executionResult.toSpecification()));
  }
  
  
   private static final ImmutableMap<
            ErrorType, com.google.api.graphql.ErrorType>
        ERROR_TYPE_MAP =
            ImmutableMap.of(
                ErrorType.DataFetchingException,
                com.google.api.graphql.ErrorType.DATA_FETCHING_EXCEPTION,
                ErrorType.InvalidSyntax,
                com.google.api.graphql.ErrorType.INVALID_SYNTAX,
                ErrorType.ValidationError,
                com.google.api.graphql.ErrorType.VALIDATION_ERROR);

    private static ImmutableList<GraphQlError> errorsToProto(List<GraphQLError> errors) {
      return errors
          .stream()
          .map(
              error ->
                  GraphQlError.newBuilder()
                      .setMessage(error.getMessage())
                      .setType(
                          ERROR_TYPE_MAP.getOrDefault(
                              error.getErrorType(),
                              com.google.api.graphql.ErrorType.UNKNOWN))
                      .addAllLocations(
                          error
                              .getLocations()
                              .stream()
                              .map(
                                  location ->
                                      SourceLocation.newBuilder()
                                          .setLine(location.getLine())
                                          .setColumn(location.getColumn())
                                          .build())
                              .collect(ImmutableList.toImmutableList()))
                      .build())
          .collect(ImmutableList.toImmutableList());
    }

}

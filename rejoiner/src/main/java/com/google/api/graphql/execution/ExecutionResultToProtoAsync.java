package com.google.api.graphql.execution;

import com.google.api.graphql.grpc.QueryResponseToProto;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import graphql.ExecutionResult;

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
                QueryResponseToProto.buildMessage(message, executionResult.toSpecification()),
                // TODO: fill in errors
                ImmutableList.of()));
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
}

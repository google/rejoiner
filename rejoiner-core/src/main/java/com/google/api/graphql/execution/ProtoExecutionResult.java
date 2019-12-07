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

import com.google.api.graphql.GraphqlError;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;

/** GraphQL execution result represented using a generated proto for the message. */
@AutoValue
@Immutable
public abstract class ProtoExecutionResult<T extends Message> {

  static <T extends Message> ProtoExecutionResult<T> create(
      T message, ImmutableList<GraphqlError> errors) {
    return new AutoValue_ProtoExecutionResult(message, errors);
  }

  /** Returns the message */
  public abstract T message();

  /** Returns errors or an empty list. */
  public abstract ImmutableList<GraphqlError> errors();
}

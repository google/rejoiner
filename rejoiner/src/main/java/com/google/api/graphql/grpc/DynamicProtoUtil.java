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

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import graphql.ExecutionResult;
import java.util.Map;

/** Utility to encode and decode data dynamically using a Proto Descriptor. */
public final class DynamicProtoUtil {

  private DynamicProtoUtil() {}

  /**
   * Encodes the data portion of an ExecutionResult as ByteString.
   *
   * <p>The FileDescriptorSet must contain a message with the name "{operationName}Response". This
   * message will be populated with data from the execution result and encoded as a ByteString.
   */
  public static ByteString encodeResponse(
      String operationName, FileDescriptorSet fileDescriptorSet, ExecutionResult executionResult) {
    try {
      // TODO: Support multiple FileDescriptors in FileDescriptorSet
      FileDescriptor fileDescriptor =
          FileDescriptor.buildFrom(fileDescriptorSet.getFileList().get(0), new FileDescriptor[] {});

      Descriptor messageType = fileDescriptor.findMessageTypeByName(operationName + "Response");

      Message message = DynamicMessage.parseFrom(messageType, ByteString.EMPTY);
      Message responseData = QueryResponseToProto.buildMessage(message, executionResult.getData());

      return responseData.toByteString();
    } catch (DescriptorValidationException | InvalidProtocolBufferException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a Map containing data from a ByteString that is parsed using the Proto Descriptor.
   *
   * <p>The FileDescriptorSet must contain a message with the name "{operationName}Request". This
   * message will be used to parse the ByteString, and the resulting message will be transformed and
   * returned as a Map.
   */
  public static Map<String, Object> decodeVariables(
      String operationName, FileDescriptorSet fileDescriptorSet, ByteString encodedRequest) {
    try {
      // TODO: Support multiple FileDescriptors in FileDescriptorSet
      FileDescriptor fileDescriptor =
          FileDescriptor.buildFrom(fileDescriptorSet.getFileList().get(0), new FileDescriptor[] {});
      Descriptor messageType = fileDescriptor.findMessageTypeByName(operationName + "Request");
      Message message = DynamicMessage.parseFrom(messageType, encodedRequest);
      return ProtoToMap.messageToMap(message);
    } catch (DescriptorValidationException | InvalidProtocolBufferException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}

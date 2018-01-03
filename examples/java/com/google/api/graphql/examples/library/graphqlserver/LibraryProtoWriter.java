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

package com.google.api.graphql.examples.library.graphqlserver;

import com.google.api.graphql.grpc.SchemaToProto;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

final class LibraryProtoWriter {
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      throw new RuntimeException("output path required");
    }
    String proto = SchemaToProto.toProto(LibrarySchema.SCHEMA);
    BufferedWriter writer = Files.newWriter(new File(args[0]), Charsets.UTF_8);
    try {
      writer.write(proto);
    } finally {
      writer.close();
    }
  }
}

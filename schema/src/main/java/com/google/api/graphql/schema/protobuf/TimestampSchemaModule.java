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

package com.google.api.graphql.schema.protobuf;

import com.google.api.graphql.rejoiner.Arg;
import com.google.api.graphql.rejoiner.SchemaModification;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.ZoneId;

public final class TimestampSchemaModule extends SchemaModule {

  @SchemaModification(addField = "iso", onType = Timestamp.class)
  String isoString(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds()).toString();
  }

  @SchemaModification(addField = "afterNow", onType = Timestamp.class)
  Boolean isAfterNow(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds()).isAfter(Instant.now());
  }

  @SchemaModification(addField = "afterNow", onType = Timestamp.class)
  String localTime(Timestamp timestamp, @Arg("timezone") String timezone) {
    return Instant.ofEpochSecond(timestamp.getSeconds()).atZone(ZoneId.of(timezone)).toString();
  }
}

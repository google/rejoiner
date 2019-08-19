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

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;

import com.google.api.graphql.rejoiner.PersonOuterClass;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.FieldMask;
import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironmentImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link SelectorToFieldMask}. */
@RunWith(JUnit4.class)
public final class SelectorToFieldMaskTest {

  @Test
  public void getFieldMaskForProtoShouldReturnEmptyFieldMaskForEmptySelector() {
    assertThat(
            SelectorToFieldMask.getFieldMaskForProto(
                    DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build(),
                    PersonOuterClass.Person.getDescriptor())
                .build())
        .isEqualTo(FieldMask.newBuilder().build());
  }

  @Test
  public void getFieldMaskForProtoShouldReturnSingleField() {
    assertThat(
            SelectorToFieldMask.getFieldMaskForProto(
                    DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                        .mergedField(
                            MergedField.newMergedField()
                                .addField(
                                    new Field(
                                        "top_level_field",
                                        new SelectionSet(ImmutableList.of(new Field("username")))))
                                .build())
                        .build(),
                   PersonOuterClass.Person.getDescriptor())
                .build())
        .isEqualTo(FieldMask.newBuilder().addPaths("username").build());
  }

  @Test
  public void getFieldMaskForProtoShouldIgnoreSelectorsNotInProto() {
    assertThat(
            SelectorToFieldMask.getFieldMaskForProto(
                    DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                        .mergedField(
                            MergedField.newMergedField()
                                .addField(
                                    new Field(
                                        "top_level_field",
                                        new SelectionSet(
                                            ImmutableList.of(
                                                new Field("username"),
                                                new Field("notInPersonProto")))))
                                .build())
                        .build(),
                   PersonOuterClass.Person.getDescriptor())
                .build())
        .isEqualTo(FieldMask.newBuilder().addPaths("username").build());
  }

  @Test
  public void getFieldMaskForProtoShouldReturnNestedField() {
    assertThat(
            SelectorToFieldMask.getFieldMaskForProto(
                    DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                        .mergedField(
                            MergedField.newMergedField()
                                .addField(
                                    new Field(
                                        "top_level_field",
                                        new SelectionSet(
                                            ImmutableList.of(
                                                new Field(
                                                    "birthday",
                                                    new SelectionSet(
                                                        ImmutableList.of(new Field("month"))))))))
                                .build())
                        .build(),
                   PersonOuterClass.Person.getDescriptor())
                .build())
        .isEqualTo(FieldMask.newBuilder().addPaths("birthday.month").build());
  }

  @Test
  public void getFieldMaskForProtoShouldReturnMultipleNestedField() {
    assertThat(
            SelectorToFieldMask.getFieldMaskForProto(
                    DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                        .mergedField(
                            MergedField.newMergedField()
                                .addField(
                                    new Field(
                                        "top_level_field",
                                        new SelectionSet(
                                            ImmutableList.of(
                                                new Field(
                                                    "birthday",
                                                    new SelectionSet(
                                                        ImmutableList.of(
                                                            new Field("day"),
                                                            new Field("month"))))))))
                                .build())
                        .build(),
                   PersonOuterClass.Person.getDescriptor())
                .build())
        .isEqualTo(
            FieldMask.newBuilder().addPaths("birthday.day").addPaths("birthday.month").build());
  }

  @Test
  public void getFieldMaskForProtoShouldFallbackToStarPathIfSubSelectorNameDoesntMatchProto() {
    assertThat(
            SelectorToFieldMask.getFieldMaskForProto(
                    DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                        .mergedField(
                            MergedField.newMergedField()
                                .addField(
                                    new Field(
                                        "top_level_field",
                                        new SelectionSet(
                                            ImmutableList.of(
                                                new Field(
                                                    "birthday",
                                                    new SelectionSet(
                                                        ImmutableList.of(new Field("unknown"))))))))
                                .build())
                        .build(),
                   PersonOuterClass.Person.getDescriptor())
                .build())
        .isEqualTo(FieldMask.newBuilder().addPaths("birthday.*").build());
  }

  @Test
  public void getFieldMaskForChildProto() {
    assertThat(
            SelectorToFieldMask.getFieldMaskForProto(
                    DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                        .mergedField(
                            MergedField.newMergedField()
                                .addField(
                                    new Field(
                                        "top_level_field", // QueryType
                                        new SelectionSet(
                                            ImmutableList.of(
                                                new Field(
                                                    "second_level_field", // RequestType
                                                    new SelectionSet(
                                                        ImmutableList.of(
                                                            new Field(
                                                                "birthday",
                                                                new SelectionSet(
                                                                    ImmutableList.of(
                                                                        new Field("month")))))))))))
                                .build())
                        .build(),
                   PersonOuterClass.Person.getDescriptor(),
                    "second_level_field")
                .build())
        .isEqualTo(FieldMask.newBuilder().addPaths("birthday.month").build());
  }
}

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

package com.google.api.graphql.rejoiner;

import com.google.cloud.firestore.v1.FirestoreClient;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

/** Unit tests for {@link GaxSchemaModule}. */
@RunWith(JUnit4.class)
public final class GaxSchemaModuleTest {

  private static final Key<Set<SchemaBundle>> KEY =
      Key.get(new TypeLiteral<Set<SchemaBundle>>() {}, Annotations.SchemaBundles.class);

  @Test
  public void schemaModuleShouldProvideQueryAndMutationFields() {
    Injector injector =
        Guice.createInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(FirestoreClient.class).toProvider(() -> null);
              }
            },
            new GaxSchemaModule() {
              @Override
              protected void configureSchema() {
                addQueryList(
                    serviceToFields(
                        FirestoreClient.class, ImmutableList.of("getDocument", "listDocuments")));
                addMutationList(
                    serviceToFields(
                        FirestoreClient.class,
                        ImmutableList.of("createDocument", "updateDocument", "deleteDocument")));
              }
            });

    SchemaBundle schemaBundle = SchemaBundle.combine(injector.getInstance(KEY));
    assertThat(schemaBundle.queryFields()).hasSize(2);
    assertThat(schemaBundle.mutationFields()).hasSize(3);
    assertThat(schemaBundle.modifications()).isEmpty();
  }
}

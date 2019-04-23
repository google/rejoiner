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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Annotation that marks fields to be included in the root query object. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Query {

  /** Name of the Query, only used when annotating a method. */
  String value() default "";
  /**
   * Full service name (including package) to be able to find appropriate metadata in generated
   * descriptor set.
   */
  String fullName() default "";
}

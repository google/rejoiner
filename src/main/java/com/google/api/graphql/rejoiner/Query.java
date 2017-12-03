package com.google.api.graphql.rejoiner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Annotation that marks fields to be included in the root query object. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Query {

  /** Name of the Query, only used when annotating a method. */
  String value() default "";
}

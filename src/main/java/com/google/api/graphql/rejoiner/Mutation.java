package com.google.api.graphql.rejoiner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Annotation that marks mutation fields to be included in the root mutation object. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Mutation {
  /** Name of the Mutation, only used when annotating a method. */
  String value() default "";
}

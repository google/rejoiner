package com.google.api.graphql.rejoiner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Annotation that marks type modification fields, or methods. */
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaModification {

  /** Name of the new field, only used when annotating a method. */
  String addField() default "";

  /** Proto reference for the type to modify, only used when annotating a method. */
  Class<?> onType() default SchemaModification.class;
}

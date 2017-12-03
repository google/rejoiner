package com.google.api.graphql.rejoiner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/** Package-private binding annotations. */
final class Annotations {

  private Annotations() {}

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface Mutations {}

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface Queries {}

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface GraphModifications {}

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface ExtraTypes {}
}

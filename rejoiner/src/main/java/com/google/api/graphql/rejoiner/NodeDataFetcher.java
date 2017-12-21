package com.google.api.graphql.rejoiner;

import java.util.function.Function;

abstract class NodeDataFetcher implements Function<String, Object> {
  private final String className;

  NodeDataFetcher(String className) {
    this.className = className;
  }

  String getClassName() {
    return className;
  }
}

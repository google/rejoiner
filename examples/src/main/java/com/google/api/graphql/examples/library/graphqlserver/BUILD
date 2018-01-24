package(default_visibility = ["//visibility:public"])

java_library(
    name = "graphqlserver",
    srcs = glob(["*.java"]),
    deps = [
        "@com_google_guava_guava2//jar", # this must be on top
        "//examples/src/main/proto/library:book_java_proto",
        "//examples/src/main/proto/library:book_service_java_proto",
        "//examples/src/main/proto/library:book_java_grpc",
        "//examples/src/main/proto/library:shelf_java_proto",
        "//examples/src/main/proto/library:shelf_service_java_proto",
        "//examples/src/main/proto/library:shelf_java_grpc",
        "//execution",
        "//rejoiner",
        "//rejoiner/src/main/java/com/google/api/graphql/grpc",
        "@antlr//jar",
        "@aop//jar",
        "@com_google_guice_multibindings//jar",
        "@com_google_protobuf_java2//jar",
        "@com_graphql_java//jar",
        "@databind//jar",
        "@future_converter//jar",
        "@future_converter_common//jar",
        "@future_converter_java8_common//jar",
        "@future_converter_guava_common//jar",
        "@grpc_java//core",
        "@grpc_java//netty",
        "@gson//jar",
        "@guice//jar",
        "@javax_annotations//jar",
        "@javax_inject//jar",
        "@javax_servlet_javax_servlet_api//jar",
        "@okhttp//jar",
        "@org_eclipse_jetty_jetty_http//jar",
        "@org_eclipse_jetty_jetty_io//jar",
        "@org_eclipse_jetty_jetty_server//jar",
        "@org_eclipse_jetty_jetty_util//jar",
        "@slf4j_api//jar",
        "@slf4j_simple//jar",
    ],
)

java_binary(
  name = "LibraryProtoWriter",
  main_class="com.google.api.graphql.examples.library.graphqlserver.LibraryProtoWriter",
  runtime_deps = [
    ":graphqlserver",
  ],
)

load("//rejoiner:rejoiner.bzl", "graphql_proto")
graphql_proto(
    name="schema",
    proto_writer = ":LibraryProtoWriter",
)

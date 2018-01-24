package(default_visibility = ["//visibility:public"])

java_binary(
    name = "graphqlserver",
    testonly = 1,
    main_class = "com.google.api.graphql.examples.streaming.graphqlserver.GraphQlGrpcServer",
    runtime_deps = [
        ":backend_lib",
        "@grpc_java//netty",
    ],
)
java_library(
    name = "backend_lib",
    srcs = glob(["*.java"]),
    deps = [
        "@com_google_guava_guava2//jar", # this must be on top
        "@com_google_protobuf_java//:protobuf_java_util",
        "@com_google_protobuf_java2//jar",
        "//examples/src/main/proto:graphql_service_java_grpc",
        "//examples/src/main/proto:graphql_service_java_proto",
        "//examples/src/main/proto:helloworld_streaming_java_grpc",
        "//examples/src/main/proto:helloworld_streaming_java_proto",
        "//execution",
        "//rejoiner",
        "//rejoiner/src/main/java/com/google/api/graphql/grpc",
        "@grpc_java//stub",
        "@antlr//jar",
        "@aop//jar",
        "@com_google_guice_multibindings//jar",
        "@com_graphql_java//jar",
        "@databind//jar",
        "@future_converter//jar",
        "@grpc_java//core",
        "@grpc_java//netty",
        "@gson//jar",
        "@guice//jar",
        "@javax_annotations//jar",
        "@javax_inject//jar",
        "@slf4j_api//jar",
        "@slf4j_simple//jar",
    ],
)

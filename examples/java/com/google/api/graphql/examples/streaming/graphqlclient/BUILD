package(default_visibility = ["//visibility:public"])

java_binary(
    name = "graphqlclient",
    testonly = 1,
    main_class = "com.google.api.graphql.examples.streaming.graphqlclient.GraphQlGrpcClient",
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
        "//examples/proto:graphql_service_java_grpc",
        "//examples/proto:graphql_service_java_proto",
        "//examples/proto:helloworld_streaming_java_grpc",
        "//examples/proto:helloworld_streaming_java_proto",
        "//execution",
        "//rejoiner",
        "@grpc_java//stub",
        "@antlr//jar",
        "@aop//jar",
        "@com_google_guice_multibindings//jar",
        "@com_google_protobuf_java2//jar",
        "@com_graphql_java//jar",
        "@databind//jar",
        "@future_converter//jar",
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

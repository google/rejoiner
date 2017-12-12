package(default_visibility = ["//visibility:public"])

load("@dependencies//:generate_workspace.bzl", "generated_java_libraries")
generated_java_libraries()

load("@grpc_java//:java_grpc_library.bzl", "java_grpc_library")

java_binary(
    name = "graphql_server",
    main_class="com.google.api.graphql.examples.grpcclient.GraphQlServer",
    srcs = glob(["java/com/google/api/graphql/examples/grpcclient/*.java"]),
    data = glob(["resources/*"]),
    deps = [
        "@com_google_guava_guava2//jar", # this must be on top
        ":helloworld_java_grpc",
        ":helloworld_java_proto",
        "//execution",
        "//rejoiner",
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

proto_library(
    name = "helloworld_proto",
    srcs = ["proto/helloworld.proto"],
)

java_proto_library(
    name = "helloworld_java_proto",
    deps = [":helloworld_proto"],
)

java_grpc_library(
    name = "helloworld_java_grpc",
    srcs = [":helloworld_proto"],
    deps = [":helloworld_java_proto"],
)

java_binary(
    name = "helloworld_server",
    testonly = 1,
    main_class = "com.google.api.graphql.examples.grpcserver.HelloWorldServer",
    runtime_deps = [
        ":helloworld_server_lib",
        "@grpc_java//netty",
    ],
)

java_library(
    name = "helloworld_server_lib",
    testonly = 1,
    srcs = glob(["java/com/google/api/graphql/examples/grpcserver/*.java"]),
    deps = [
        ":helloworld_java_grpc",
        ":helloworld_java_proto",
        "@com_google_api_grpc_proto_google_common_protos//jar",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_guava_guava//jar",
        "@com_google_protobuf_java//:protobuf_java",
        "@com_google_protobuf_java//:protobuf_java_util",
        "@grpc_java//core",
        "@grpc_java//protobuf",
        "@grpc_java//stub",
    ],
)

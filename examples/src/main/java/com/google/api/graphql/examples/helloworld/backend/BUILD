package(default_visibility = ["//visibility:public"])

load("@dependencies//:generate_workspace.bzl", "generated_java_libraries")
generated_java_libraries()

java_binary(
    name = "backend",
    testonly = 1,
    main_class = "com.google.api.graphql.examples.helloworld.backend.HelloWorldServer",
    runtime_deps = [
        ":backend_lib",
        "@grpc_java//netty",
    ],
)

java_library(
    name = "backend_lib",
    testonly = 1,
    srcs = glob(["*.java"]),
    deps = [
        "//examples/src/main/proto:helloworld_java_grpc",
        "//examples/src/main/proto:helloworld_java_proto",
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

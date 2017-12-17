package(default_visibility = ["//visibility:public"])

java_library(
    name = "backend",
    testonly = 1,
    srcs = glob(["*.java"]),
    deps = [
        "//examples/proto:helloworld_java_grpc",
        "//examples/proto:helloworld_java_proto",
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

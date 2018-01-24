package(default_visibility = ["//visibility:public"])

load("@grpc_java//:java_grpc_library.bzl", "java_grpc_library")

proto_library(
    name = "helloworld_proto",
    srcs = ["helloworld.proto"],
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

proto_library(
    name = "helloworld_streaming_proto",
    srcs = ["helloworld_streaming.proto"],
)

java_proto_library(
    name = "helloworld_streaming_java_proto",
    deps = [":helloworld_streaming_proto"],
)

java_grpc_library(
    name = "helloworld_streaming_java_grpc",
    srcs = [":helloworld_streaming_proto"],
    deps = [":helloworld_streaming_java_proto"],
)

proto_library(
    name = "graphql_service_proto",
    srcs = ["graphql_service.proto"],
    deps = [
      "@com_google_protobuf//:struct_proto",
    ]
)

java_proto_library(
    name = "graphql_service_java_proto",
    deps = [":graphql_service_proto"],
)

java_grpc_library(
    name = "graphql_service_java_grpc",
    srcs = [":graphql_service_proto"],
    deps = [":graphql_service_java_proto"],
)

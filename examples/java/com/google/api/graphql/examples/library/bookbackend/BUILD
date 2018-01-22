package(default_visibility = ["//visibility:public"])

java_binary(
    name = "bookbackend",
    testonly = 1,
    main_class = "com.google.api.graphql.examples.library.bookbackend.BookServer",
    runtime_deps = [
        ":bookbackend_lib",
        "@grpc_java//netty",
    ],
)
java_library(
    name = "bookbackend_lib",
    testonly = 1,
    srcs = glob(["*.java"]),
    deps = [
        "@com_google_guava_guava2//jar", # this must be on top
        "//examples/proto/library:book_java_grpc",
        "//examples/proto/library:book_java_proto",
        "//examples/proto/library:book_service_java_proto",
        "@com_google_api_grpc_proto_google_common_protos//jar",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_protobuf_java//:protobuf_java",
        "@com_google_protobuf_java//:protobuf_java_util",
        "@grpc_java//core",
        "@grpc_java//protobuf",
        "@grpc_java//stub",
    ],
)

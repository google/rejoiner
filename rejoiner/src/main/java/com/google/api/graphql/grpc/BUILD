package(default_visibility = ["//visibility:public"])

java_library(
    name = "grpc",
    srcs = glob(["*.java"]),
    deps = [
        ":auto",
        "@aop//jar",
        "@com_google_guava_guava2//jar",
        "@com_google_guice_multibindings//jar",
        "@com_google_protobuf_java2//jar",
        "@com_graphql_java//jar",
        "@grpc_java//core",
        "@com_google_protobuf_java//:protobuf_java_util",
        "@grpc_java//stub",
        "@guice//jar",
        "@javax_annotations//jar",
        "@javax_inject//jar",
    ],
)

java_plugin(
    name = "auto_plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = ["@com_google_autovalue//jar"],
)

java_library(
    name = "auto",
    exported_plugins = [":auto_plugin"],
    exports = ["@com_google_autovalue//jar"],
)

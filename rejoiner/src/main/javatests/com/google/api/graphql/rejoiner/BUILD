package(default_visibility = ["//visibility:public"])

java_test(
    name = "RejoinerIntegrationTest",
    srcs = ["RejoinerIntegrationTest.java"],
    test_class = "com.google.api.graphql.rejoiner.RejoinerIntegrationTest",
    deps = [
        ":greetings_java_proto",
        ":test_java_proto",
        "//rejoiner",
        "@com_google_guava_guava//jar",
        "@com_google_guice//jar",
        "@com_graphql_java//jar",
        "@junit//jar",
        "@truth//jar",
        "@truth_extension//jar",
    ],
)

java_test(
    name = "ProtoRegistryTest",
    srcs = ["ProtoRegistryTest.java"],
    test_class = "com.google.api.graphql.rejoiner.ProtoRegistryTest",
    deps = [
        ":greetings_java_proto",
        ":test_java_proto",
        "//rejoiner",
        "@com_google_guava_guava//jar",
        "@com_graphql_java//jar",
        "@junit//jar",
        "@truth//jar",
        "@truth_extension//jar",
    ],
)

java_test(
    name = "TypeTest",
    srcs = ["TypeTest.java"],
    test_class = "com.google.api.graphql.rejoiner.TypeTest",
    deps = [
        ":greetings_java_proto",
        ":test_java_proto",
        "//rejoiner",
        "@com_graphql_java//jar",
        "@junit//jar",
        "@truth//jar",
        "@truth_extension//jar",
    ],
)

java_test(
    name = "GqlInputConverterTest",
    srcs = ["GqlInputConverterTest.java"],
    test_class = "com.google.api.graphql.rejoiner.GqlInputConverterTest",
    deps = [
        ":greetings_java_proto",
        ":test_java_proto",
        "//rejoiner",
        "@com_google_guava_guava//jar",
        "@com_google_protobuf//:protobuf_java",
        "@com_graphql_java//jar",
        "@junit//jar",
        "@truth//jar",
        "@truth_extension//jar",
        "@truth_extension_lite//jar",
    ],
)

java_test(
    name = "ProtoToGqlTest",
    srcs = ["ProtoToGqlTest.java"],
    test_class = "com.google.api.graphql.rejoiner.ProtoToGqlTest",
    deps = [
        ":greetings_java_proto",
        ":test_java_proto",
        "//rejoiner",
        "@com_graphql_java//jar",
        "@junit//jar",
        "@truth//jar",
    ],
)

java_test(
    name = "SchemaProviderModuleTest",
    srcs = ["SchemaProviderModuleTest.java"],
    test_class = "com.google.api.graphql.rejoiner.SchemaProviderModuleTest",
    deps = [
        ":greetings_java_proto",
        ":test_java_proto",
        "//rejoiner",
        "@com_google_guava_guava//jar",
        "@com_google_guice//jar",
        "@com_graphql_java//jar",
        "@junit//jar",
        "@truth//jar",
    ],
)

java_test(
    name = "SchemaModuleTest",
    srcs = ["SchemaModuleTest.java"],
    test_class = "com.google.api.graphql.rejoiner.SchemaModuleTest",
    deps = [
        ":greetings_java_proto",
        ":test_java_proto",
        "//rejoiner",
        "@com_google_guava_guava//jar",
        "@com_google_guice//jar",
        "@com_graphql_java//jar",
        "@com_google_protobuf//:protobuf_java",
        "@junit//jar",
        "@truth//jar",
    ],
)

proto_library(
    name = "test_proto",
    srcs = ["test_proto.proto"],
    deps = [],
)

java_proto_library(
    name = "test_java_proto",
    deps = [":test_proto"],
)

proto_library(
    name = "greetings_proto",
    srcs = ["greetings.proto"],
    deps = [],
)

java_proto_library(
    name = "greetings_java_proto",
    deps = [":greetings_proto"],
)

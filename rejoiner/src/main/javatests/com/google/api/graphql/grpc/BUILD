package(default_visibility = ["//visibility:public"])

java_test(
    name = "SchemaToProtoTest",
    srcs = ["SchemaToProtoTest.java"],
    test_class = "com.google.api.graphql.grpc.SchemaToProtoTest",
    deps = [
        "//rejoiner/src/main/javatests/com/google/api/graphql/rejoiner:greetings_java_proto",
        "//rejoiner/src/main/javatests/com/google/api/graphql/rejoiner:test_java_proto",
        "//rejoiner:grpc",
        "@com_google_guava_guava//jar",
        "@com_google_guice//jar",
        "@com_graphql_java//jar",
        "@junit//jar",
        "@truth//jar",
        "@truth_extension//jar",
        "@truth_extension_lite//jar",
    ],
)

java_test(
    name = "QueryResponseToProtoTest",
    srcs = ["QueryResponseToProtoTest.java"],
    test_class = "com.google.api.graphql.grpc.QueryResponseToProtoTest",
    deps = [
        "//rejoiner/src/main/javatests/com/google/api/graphql/rejoiner:greetings_java_proto",
        "//rejoiner/src/main/javatests/com/google/api/graphql/rejoiner:test_java_proto",
        "//rejoiner:grpc",
        "@com_google_guava_guava//jar",
        "@com_google_guice//jar",
        "@com_graphql_java//jar",
        "@junit//jar",
        "@truth//jar",
        "@truth_extension//jar",
        "@truth_extension_lite//jar",
    ],
)


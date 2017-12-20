package(default_visibility = ["//visibility:public"])

proto_library(
  name = "relay_proto",
  srcs = ["relay.proto"],
  deps = [
    "@com_google_protobuf//:descriptor_proto",
  ],
)

java_proto_library(
  name = "relay_java_proto",
  deps = [":relay_proto"]
)
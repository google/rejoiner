package(default_visibility = ["//visibility:public"])

load("@grpc_java//:java_grpc_library.bzl", "java_grpc_library")

# Book Service
proto_library(
  name = "book_proto",
  srcs = ["book.proto"],
)
java_proto_library(
    name = "book_java_proto",
    deps = [":book_proto"],
)
proto_library(
  name = "book_service_proto",
  srcs = ["book_service.proto"],
  deps = [
    ":book_proto",
    "@com_google_protobuf//:empty_proto",
  ],
)
java_proto_library(
  name = "book_service_java_proto",
  deps = [
    ":book_service_proto",
    ":book_proto"
  ],
)
java_grpc_library(
  name = "book_java_grpc",
  srcs = [":book_service_proto"],
  deps = [":book_service_java_proto"],
)

# Shelf Service
proto_library(
  name = "shelf_proto",
  srcs = ["shelf.proto"],
)
java_proto_library(
    name = "shelf_java_proto",
    deps = [":shelf_proto"],
)
proto_library(
  name = "shelf_service_proto",
  srcs = ["shelf_service.proto"],
  deps = [
    ":shelf_proto",
    "@com_google_protobuf//:empty_proto",
  ],
)
java_proto_library(
  name = "shelf_service_java_proto",
  deps = [
    ":shelf_service_proto",
    ":shelf_proto"
  ],
)
java_grpc_library(
  name = "shelf_java_grpc",
  srcs = [":shelf_service_proto"],
  deps = [":shelf_service_java_proto"],
)
maven_jar(
  name = "com_graphql_java",
  artifact = "com.graphql-java:graphql-java:6.0",
)
maven_jar(
  name = "com_google_guice",
  artifact = "com.google.inject:guice:jar:4.1.0",
)
maven_jar(
  name = "javax_inject",
  artifact = "javax.inject:javax.inject:jar:1",
)
maven_jar(
  name = "com_google_autovalue",
  artifact = "com.google.auto.value:auto-value:jar:1.5.2",
)
maven_jar(
  name = "javax_annotations",
  artifact = "com.google.code.findbugs:jsr305:3.0.0",
)
maven_jar(
  name = "truth",
  artifact = "com.google.truth:truth:0.36",
)
maven_jar(
  name = "truth_extension_lite",
  artifact = "com.google.truth.extensions:truth-liteproto-extension:jar:0.36",
)
maven_jar(
  name = "truth_extension",
  artifact = "com.google.truth.extensions:truth-proto-extension:jar:0.36",
)
maven_jar(
  name = "junit",
  artifact = "junit:junit:jar:4.12",
)
maven_jar(
  name = "aop",
  artifact = "aopalliance:aopalliance:jar:1.0",
)
maven_jar(
  name = "future_converter",
  artifact = "net.javacrumbs.future-converter:future-converter-java8-guava:1.1.0"
)
maven_jar(
  name = "com_google_guava_guava2",
  artifact = "com.google.guava:guava:23.5-jre",
)
maven_jar(
  name = "antlr",
  artifact = "org.antlr:antlr4-runtime:jar:4.5.1",
)
maven_jar(
  name = "com_google_protobuf_java2",
  artifact = "com.google.protobuf:protobuf-java:jar:3.5.0",
)
maven_jar(
  name = "guice",
  artifact = "com.google.inject:guice:jar:4.1.0",
)
maven_jar(
  name = "com_google_guice_multibindings",
  artifact = "com.google.inject.extensions:guice-multibindings:jar:4.1.0",
)
maven_jar(
  name = "databind",
  artifact = "com.fasterxml.jackson.core:jackson-databind:2.8.8.1"
)
maven_jar(
  name = "gson",
  artifact = "com.google.code.gson:gson:2.8.0",
)
maven_jar(
  name = "okhttp",
  artifact = "com.squareup.okhttp3:okhttp:3.8.0",
)
maven_jar(
  name = "slf4j_simple",
  artifact = "org.slf4j:slf4j-simple:1.7.25",
)
maven_jar(
  name = "slf4j_api",
  artifact = "org.slf4j:slf4j-api:1.7.25",
)

# proto_library rules implicitly depend on @com_google_protobuf//:protoc,
# which is the proto-compiler.
# This statement defines the @com_google_protobuf repo.
# This is explicitly added to get the built-in known protos.
# https://github.com/google/protobuf/commit/699c0eb9cf6573f3a00b4db61f60aff92dc3dd7a
http_archive(
  name = "com_google_protobuf",
  urls = ["https://github.com/google/protobuf/archive/master.zip"],
  strip_prefix = "protobuf-master",
)

############ transitive_maven_jar ############

http_archive(
  name = "trans_maven_jar",
  url = "https://github.com/bazelbuild/migration-tooling/archive/54a0ffb171f1f8e3da928619395df2328e2f5e1c.zip",
  type = "zip",
  strip_prefix = "migration-tooling-54a0ffb171f1f8e3da928619395df2328e2f5e1c",
)

load("@trans_maven_jar//transitive_maven_jar:transitive_maven_jar.bzl", "transitive_maven_jar")
transitive_maven_jar(
  name = "dependencies",
  artifacts = [
    "org.eclipse.jetty:jetty-server:9.3.8.v20160314",
    # static resources are not found for newer version
    # "org.eclipse.jetty:jetty-server:jar:9.4.8.v20171121",
  ]
)

load("@dependencies//:generate_workspace.bzl", "generated_maven_jars")
generated_maven_jars()

############ git_repository ############

git_repository(
  name = "grpc_java",
  remote = "https://github.com/grpc/grpc-java.git",
  commit = "8a210d037dec14c0da687d639d6601b0d8dd1fb3",
)
load("@grpc_java//:repositories.bzl", "grpc_java_repositories")
grpc_java_repositories(omit_com_google_protobuf = True)

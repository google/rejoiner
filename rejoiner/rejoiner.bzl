# Copyright 2017 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Rules for generating a GraphQL service proto.

To use this rule, add a load() statement at the top of your BUILD file, for example:

load("//rejoiner:rejoiner.bzl", "graphql_proto")

# java binary that writes the schema proto file.
java_binary(
  name = "LibraryProtoWriter",
  main_class="com.google.api.graphql.examples.library.graphqlserver.LibraryProtoWriter",
  runtime_deps = [":graphqlserver"],
)

# runs the proto writer as a build rule so the resulting proto can be an input to other build rules
graphql_proto(
    name="schema",
    proto_writer = ":LibraryProtoWriter",
)
"""

def _impl(ctx):
  # The list of arguments we pass to the script.
  args = [ctx.outputs.out.path]
  # Action to call the script.
  ctx.actions.run(
      outputs=[ctx.outputs.out],
      arguments=args,
      progress_message="Genearting GraphQL proto: %s" % ctx.outputs.out.short_path,
      executable=ctx.executable.proto_writer)

graphql_proto = rule(
  implementation=_impl,
  attrs={
      "proto_writer": attr.label(cfg="host", mandatory=True, allow_files=True, executable=True),
  },
  outputs = {"out": "%{name}.proto"}
)
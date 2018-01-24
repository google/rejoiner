---
id: examples
title: Examples
---

## Helloworld HTTP Server

This example showcases generating a GraphQL schema from a simple gRPC service.
The GraphQL server makes rpc calls to the backend server.

To run the example you need to run the backend gRPC server first, then run the
GraphQL frontend server.

1. Build and run the gRPC backend server
   <p>From the top level directory run these commands:

    ```
    > bazel run examples/java/com/google/api/graphql/examples/helloworld/backend --script_path server.sh
    > ./server.sh
    ```
2. Run the GraphQL frontend server
   <p>In another terminal run the following command:

   ```
   > bazel run examples:helloworld_graphqlserver
   ```

3. Visit GraphiQL and make requests

   - [http://localhost:8080](http://localhost:8080)


## Streaming GraphQL using gRPC

This example showcases a streaming GraphQL response. The goal is to support the `@streaming` directive.

Note: This is a work in progress

1. Run the gRPc backend server

   ```
   >  bazel run examples/java/com/google/api/graphql/examples/streaming/backend --script_path backend.sh
   > ./backend.sh
   ```
2. Run the GraphQL frontend gRPC server

   ```
   > bazel run examples/java/com/google/api/graphql/examples/streaming/graphqlserver --script_path graphql_server.sh
   > ./graphql_server.sh
   ```

3. Run the GraphQL gRPC client

   ```
   > bazel run examples/java/com/google/api/graphql/examples/streaming/graphqlclient
   ```

Current Limitations of Streaming Demo

 - The response is always streaming (it doesn't depend on the presents of the @streaming directive)
 - The demo is using a gRPC service for the GraphQL API (no HTTP/JSON demo).
 - The data field of the top level GraphQL Response proto is a JSON encoded string of the response (it could be either Any or oneOf())
 - Variables are not used in the demo
 - Errors are not returned in the response

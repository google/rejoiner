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
    > cd examples-gradle
    > ./gradlew installDist
    > ./build/install/examples/bin/helloworld-backend

    ```
2. Run the GraphQL frontend server
   <p>In another terminal run the following command:

   ```
   > ./build/install/examples/bin/helloworld-graphqlserver
   ```

3. Visit GraphiQL and make requests

   - [http://localhost:8080](http://localhost:8080)


## Streaming GraphQL using gRPC

This example showcases a streaming GraphQL response. The goal is to support the `@streaming` directive.

Note: This is a work in progress

1. Run the gRPc backend server

   ```
   > cd examples-gradle
   > ./gradlew installDist
   > ./build/install/examples/bin/streaming-backend
   ```
2. Run the GraphQL frontend gRPC server

   ```
   > cd examples-gradle
   > ./build/install/examples/bin/streaming-graphql-server
   ```

3. Run the GraphQL gRPC client

   ```
   > cd examples-gradle
   > ./build/install/examples/bin/streaming-graphql-client
   ```

Current Limitations of Streaming Demo

 - The response is always streaming (it doesn't depend on the presents of the @streaming directive)
 - The demo is using a gRPC service for the GraphQL API (no HTTP/JSON demo).
 - The data field of the top level GraphQL Response proto is a JSON encoded string of the response (it could be either Any or oneOf())
 - Variables are not used in the demo
 - Errors are not returned in the response


## Library Example

The library example uses DataLoaders which provides a request-scoped cache and automatically batches requests.


## Example using Google cloud / firebase schema modules

There are a few pre configured schema modules in `schema`. This example installs those
schema and provides access using GraphiQL using a Jetty HTTP server.

```
cd examples/schema
mvn jetty:run
```

## Community Examples

[github.com/dbaggett/medallion](https://github.com/dbaggett/medallion) contains multiple microservices and uses Kotlin instead of Java.

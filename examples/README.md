# Rejoiner Examples


## Hello-gRPC

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

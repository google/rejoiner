---
id: queries
title: Queries and Mutations
---

## GraphQL Query

```java
final class TodoQuerySchemaModule extends SchemaModule {
  @Query("listTodo")
  ListenableFuture<ListTodoResponse> listTodo(ListTodoRequest request, TodoClient todoClient) {
    return todoClient.listTodo(request);
  }
}
```

In this example `request` is of type `ListTodoRequest` (a protobuf message), so
it's used as a parameter in the generated GraphQL query. `todoService` isn't a
protobuf message, so it's provided by the Guice injector.

This is useful for providing rpc services or database access objects for
fetching data. Authentication data can also be provided here.

Common implementations for these annotated methods:
 - Make gRPC calls to microservices which can be implemented in any language
 - Load protobuf messages directly from storage
 - Perform arbitrary logic to produce the result

## GraphQL Mutation

```java
final class TodoMutationSchemaModule extends SchemaModule {
  @Mutation("createTodo")
  ListenableFuture<Todo> createTodo(
      CreateTodoRequest request, TodoService todoService, @AuthenticatedUser String email) {
    return todoService.createTodo(request, email);
  }
}
```

## Supported return types

All generated proto messages extend `Message`.
 - Any subclass of `Message`
 - `ImmutableList<? extends Message>`
 - `ListenableFuture<? extends Message>`
 - `ListenableFuture<ImmutableList<? extends Message>>`

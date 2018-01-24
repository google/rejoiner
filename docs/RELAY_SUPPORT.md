---
id: relay
title: Relay support
---

Note: Relay support is currently a work in progress and the API may change

## Relay ID

A proto annotation is used to mark a field as the id. ID values need to be
unique for a type but does not need to be unique across types.

```
// A single book in the library.
message Book {
  // The id of the book.
  string id = 1 [(google.api.graphql.relay_options).id = true];

  // The name of the book author.
  string author = 2;

  // The title of the book.
  string title = 3;
}
```

## Node interface

To implement the node interface the `@RelayNode` annotation can be placed on
the Query method. This assumes the request type (`GetBookRequest`) has a field
called id, which is automatically populated by the framework.

```java
@Query("getBook")
@RelayNode
ListenableFuture<Book> getBook(
    GetBookRequest request, BookServiceGrpc.BookServiceFutureStub client) {
  return client.getBook(request);
}

```

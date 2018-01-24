---
id: joining
title: Joining Types
---

## Adding edges between GraphQL types

In this example we are adding a reference to the User type on the Todo type.
```java
final class TodoToUserSchemaModule extends SchemaModule {
  @SchemaModification(addField = "creator", onType = Todo.class)
  ListenableFuture<User> todoCreatorToUser(UserService userService, Todo todo) {
    return userService.getUserByEmail(todo.getCreatorEmail());
  }
}
```
In this case the Todo parameter is the parent object which can be referenced to
get the creator's email.

This is how types are joined within and across APIs.

![Rejoiner API Joining](https://github.com/google/rejoiner/raw/master/website/static/rejoiner.svg?sanitize=true)

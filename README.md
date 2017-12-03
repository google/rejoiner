# Rejoiner

Rejoiner
 - Allows the GraphQL schema to be flexibility defined and composed as shared components
 - Generates GraphQL types from Proto definitions
 - Populates request Proto based on GraphQL query parameters
 - Supplies a DSL to modify the generated schema
 - Joins data sources by annotating methods that fetch data
 - Creates Proto FieldMasks based on GraphQL selectors


## GraphQL Query

```
final class TodoQuerySchemaModule extends SchemaModule {
  @Query("listTodo")
  ListenableFuture<ListTodoResponse> listTodo(ListTodoRequest request, TodoService todoService) {
    return todoService.listTodo(request);
  }
  //...
}
```

In this example `request` is of type `ListTodoRequest` a protobuf message, so
it's used as a parameter in the generated GraphQL query. `todoService` isn't a
protobuf message, so it's injected in by Guice. This is useful for providing
rpc services or database access objects for fetching data. Authentication data
can also be provided here.

## GraphQL Mutation

```
final class TodoMutationSchemaModule extends SchemaModule {
  @Mutation("createTodo")
  ListenableFuture<Todo> createTodo(
      CreateTodoRequest request, TodoService todoService, @AuthenticatedUser String email) {
    return todoService.createTodo(request, email);
  }
}
```

## Adding edges between GraphQL types

In this example we are adding a reference to the User type on the Todo type.
```
final class TodoToUserSchemaModule extends SchemaModule {
  @SchemaModification(addField = "creator", onType = Todo.class)
  ListenableFuture<User> todoCreatorToUser(UserService userService, Todo todo) {
    return userService.getUserByEmail(todo.getCreatorEmail());
  }
}
```
In this case the Todo parameter is the parent object which can be referenced to
get the creators email.


## Building the GraphQL schema
```
import com.google.api.graphql.rejoiner.SchemaProviderModule;

public final class TodoModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new SchemaProviderModule());
    install(new TodoQuerySchemaModule());
    install(new TodoMutationSchemaModule());
    install(new TodoToUserSchemaModule());
  }
}
```

## Using the GraphQL schema

```
import com.google.api.graphql.rejoiner.Query;
import graphql.schema.GraphQLSchema;

//...

@Inject @Schema GraphQLSchema;

```

## Supported return types

 - Message
 - ImmutableList<? extends Message>
 - ListenableFuture<? extends Message>
 - ListenableFuture<ImmutableList<? extends Message>>


Note: This is not an official Google product

---
id: schema-modifications
title: Schema Modifications
---

## Removing a field

```java
final class TodoModificationsSchemaModule extends SchemaModule {
  @SchemaModification
  TypeModification removePrivateTodoData =
      Type.find(Todo.getDescriptor()).removeField("privateTodoData");
}
```

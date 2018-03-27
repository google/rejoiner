---
id: schema-modifications
title: Schema Modifications
---

## Adding a field

Fields can contain parameters, such as specifying the timezone to use when
converting a timestamp to a local time.

```java
final class TimestampSchemaModule extends SchemaModule {

  @SchemaModification(addField = "iso", onType = Timestamp.class)
  String isoString(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds()).toString();
  }

  @SchemaModification(addField = "localTime", onType = Timestamp.class)
  String localTime(Timestamp timestamp, @Arg("timezone") String timezone) {
    return Instant.ofEpochSecond(timestamp.getSeconds()).atZone(ZoneId.of(timezone)).toString();
  }
}
```

## Removing a field

```java
final class TodoModificationsSchemaModule extends SchemaModule {
  @SchemaModification
  TypeModification removePrivateTodoData =
      Type.find(Todo.getDescriptor()).removeField("privateTodoData");
}
```

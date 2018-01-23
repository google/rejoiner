---
id: implementation
title: Implementation
---

## Public API components

## SchemaModule
The main component of Rejoiner is the [SchemaModule](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/SchemaModule.java).

TODO: Explain how SchemaModule works and how it uses Guice Multibinder.

### Java Annotations
These annotations are used to build and modify a generated GraphQL schema.
 - [Query](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/Query.java)
 - [Mutation](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/Mutation.java)
 - [SchemaModification](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/SchemaModification.java)
 - [Args](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/Args.java)

### DSL
 - [Type](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/Type.java) - used to build schema modifications.

## Producing the final GraphQL schema

Each [SchemaModule](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/SchemaModule.java) generates parts of the schema and the [SchemaProviderModule](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/SchemaProviderModule.java) combines all of those parts into the final schema.

 - [Schema](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/Schema.java)
 - [SchemaProviderModule](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/SchemaProviderModule.java)

## Functional utilities

 - [SchemaToProto](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/SchemaToProto.java) - used to generate a protobuf message from the resulting schema. This can be used to expose the GraphQL endpoint over gRPC.
 - [SchemaToTypeScript](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/SchemaToTypeScript.java) - generates a basic TypeScript typedef file. Complex clients should consider GraphQL frameworks such as Relay or Apollo.
- [QueryResponseToProto](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/QueryResponseToProto.java)


## Extending Rejoiner
 - [TypeModification](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/TypeModification.java) are functions that transform GraphQLObjectType instances. This is used to add and remove fields from the generated GraphQLObjectType instances. Additional transforms can be added by implementing this interface.
 - [Annotations](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/Annotations.java)


# Implementation

These classes are package private and not part of the public API.

 - [GqlInputConverter](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/GqlInputConverter.java)
 - [ProtoRegistry](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/ProtoRegistry.java)
 - [ProtoToGql](https://github.com/google/rejoiner/tree/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/ProtoToGql.java)

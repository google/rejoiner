# Public API components

## SchemaModule
The main component of Rejoiner is the [SchemaModule](./src/main/java/com/google/api/graphql/rejoiner/SchemaModule.java).

TODO: Explain how SchemaModule works and how it uses Guice Multibinder.

### Annotations
 - [Query](./src/main/java/com/google/api/graphql/rejoiner/Query.java)
 - [Mutation](./src/main/java/com/google/api/graphql/rejoiner/Mutation.java)
 - [SchemaModification](./src/main/java/com/google/api/graphql/rejoiner/SchemaModification.java)
 - [Args](./src/main/java/com/google/api/graphql/rejoiner/Args.java)

### DSL
 - [Type](./src/main/java/com/google/api/graphql/rejoiner/Type.java) - used to build schema modifications.

## Producing the final GraphQL schema

Each [SchemaModule](./src/main/java/com/google/api/graphql/rejoiner/SchemaModule.java) generates parts of the schema and the [SchemaProviderModule](./src/main/java/com/google/api/graphql/rejoiner/SchemaProviderModule.java) combines all of those parts into the final schema.

 - [Schema](./src/main/java/com/google/api/graphql/rejoiner/Schema.java)
 - [SchemaProviderModule](./src/main/java/com/google/api/graphql/rejoiner/SchemaProviderModule.java)

## Functional utilities

 - [SchemaToProto](./src/main/java/com/google/api/graphql/rejoiner/SchemaToProto.java) - used to generate a protobuf message from the resulting schema. This can be used to expose the GraphQL endpoint over gRPC.
 - [SchemaToTypeScript](./src/main/java/com/google/api/graphql/rejoiner/SchemaToTypeScript.java) - generates a basic TypeScript typedef file. Complex clients should consider GraphQL frameworks such as Relay or Apollo.
- [QueryResponseToProto](./src/main/java/com/google/api/graphql/rejoiner/QueryResponseToProto.java)


## Extending Rejoiner
 - [TypeModification](./src/main/java/com/google/api/graphql/rejoiner/TypeModification.java) are functions that transform GraphQLObjectType instances. This is used to add and remove fields from the generated GraphQLObjectType instances. Additional transforms can be added by implementing this interface.
 - [Annotations](./src/main/java/com/google/api/graphql/rejoiner/Annotations.java)


# Implementation

These classes are package private and not part of the public API.

 - [GqlInputConverter](./src/main/java/com/google/api/graphql/rejoiner/GqlInputConverter.java)
 - [ProtoRegistry](./src/main/java/com/google/api/graphql/rejoiner/ProtoRegistry.java)
 - [ProtoToGql](./src/main/java/com/google/api/graphql/rejoiner/ProtoToGql.java)

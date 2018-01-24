---
id: fieldmasks
title: Field Mask Support
---

gRPC APIs commonly use [Field Masks](https://developers.google.com/protocol-buffers/docs/reference/java/com/google/protobuf/FieldMask)
to support projections.

Rejoiner can automatically populate a FieldMask from the GraphQL query. This
allows the fields selected in a GraphQL query to propagate from all the way to
the backend.

For example, fields selected in a GraphQL query can be passed to the
Backend API in the Field Mask parameter, which can then be used as part of a
projection when querying the database.

## Field Masks

`FieldMask` represents a set of symbolic field paths, for example:
```
   paths: "f.a"
   paths: "f.b.d"
```     

Here `f` represents a field in some root message, `a` and `b`
fields in the message found in `f`, and `d` a field found in the
message in `f.b`.

Field masks are used to specify a subset of fields that should be
returned by a get operation or modified by an update operation.

## Field Masks in Projections
When used in the context of a projection, a response message or
sub-message is filtered by the API to only contain those fields as
specified in the mask. For example, if the mask in the previous
example is applied to a response message as follows:

```
   f {
     a : 22
     b {
       d : 1
       x : 2
     }
     y : 13
   }
   z: 8
```

The result will not contain specific values for fields x,y and z
(their value will be set to the default, and omitted in proto text
output):
```
   f {
     a : 22
     b {
       d : 1
     }
   }
```

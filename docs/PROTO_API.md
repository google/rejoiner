---
id: proto-api
title: Proto GraphQL API
---

There is a proposal for [generating protobuf requensts and responses for GraphQL queries](https://github.com/google/rejoiner/issues/43)
at client build time. The client can use the proto to make a GraphQL request to the server using gRPC. The server will receive
the request proto along with the full query string and can infer the request/response protos at query time.

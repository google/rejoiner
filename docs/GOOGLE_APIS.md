---
id: googleapis
title: Google APIs
---

Many of Google APIs are accessible using gRPC. The proto
definition of those APIs are [published on GitHub](https://github.com/googleapis/googleapis),
and can be used with Rejoiner to create a GraphQL gateway to Google APIs. This
gateway server can be hosted on the Google Could Platform allowing all
roundtrips to take place within the high-speed Google network.

Calls can be made using the standard Query annotation.

```
@Query("getDocument")
ListenableFuture<Document> getDocument(
    GetDocumentRequest request, FirestoreClient client) {
  return apiFutureToListenableFuture(client.getDocumentCallable().futureCall(request));
}
```

[Java clients](https://github.com/GoogleCloudPlatform/google-cloud-java) are
published for some of these APIs, which helps with authentication and other
concerns.

The `GaxSchemaModule` provides additional support to automatically generate
queries and mutations based on RPCs in these services.

```
@Namespace("firestore")
public final class FirestoreSchemaModule extends GaxSchemaModule {

  @Override
  protected void configureSchema() {
    addQueryList(
        serviceToFields(FirestoreClient.class, ImmutableList.of("getDocument", "listDocuments")));
    addMutationList(
        serviceToFields(
            FirestoreClient.class,
            ImmutableList.of("createDocument", "updateDocument", "deleteDocument")));
  }
}
```


Additional fields can be added to types defined in these APIs.

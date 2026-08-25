# rec-proto

`rec-proto` contains the Java wire contract shared by `rec-server`, the Java SDK, and example data
loaders. It consists of serializable POJOs and enums without Spring or transport dependencies.

```xml
<dependency>
    <groupId>com.openrec</groupId>
    <artifactId>rec-proto</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Run `mvn install` from the `rec-server` root before building consumers against a local snapshot.

## Envelope

Every request and response uses a common envelope:

```java
class JsonReq<T> {
    String requestId;
    T body;
}

class JsonRes<T> {
    int code;
    boolean status;
    String msg;
    T data;
}
```

The server places `requestId` in the logging MDC. Clients should set it when they need end-to-end
trace correlation; otherwise the request model generates a UUID. `ProtoCode` defines the standard
HTTP-style result codes. `JsonResType` helps Gson deserialize `JsonRes<T>` despite type erasure.

## Entities

`Item`, `User`, and `Event` represent the records accepted by the push APIs. Time fields are strings
containing epoch seconds. `extFields` preserves application-specific attributes not covered by the
core model.

Important compatibility details:

- `Item.tags` is a comma-separated string, while `User.tags` is `List<String>`.
- Event type values are free-form, but `click`, `expose`, and `dislike` have built-in serving
  behavior for triggers and filters.
- Entity field names and serialized forms are shared contracts; update SDK and example consumers
  when changing them.

## Push contract

`ItemReq`, `UserReq`, and `EventReq` extend the same batch request:

```java
class AbstractPushReq<T> {
    PushCmd cmd = PushCmd.INSERT;
    List<T> data;
}

enum PushCmd { INSERT, UPDATE, DELETE }
```

`INSERT` and `UPDATE` are upserts in the current server implementations. `DELETE` removes the
corresponding records. The active Spring profile decides whether mutations are written directly to
Redis or published as versioned Kafka messages.

## Recommendation contract

```java
class RecommendReq {
    String scene;
    int size;
    String userId;
    String deviceId;
    List<String> itemIds;
    String type;
    boolean debug;
    String targetType;
}

class RecommendRes<T> {
    List<ScoreResult> results;
    List<T> detailInfos;
}
```

`debug=true` asks the item endpoint to attach entity details. `targetType` is assigned by the
endpoint. The item endpoint is implemented; the reserved user-recommendation endpoint currently
returns `NOT_IMPLEMENTED`.

`GraphEngine.prepare()` exposes `RecommendReq` fields to nodes by field name. Renaming one is
therefore a serving-graph contract change, not a cosmetic refactor.

### Scores and recall provenance

`ScoreResult` carries the final ordering score and the contributions used to derive it:

| Field | Meaning |
|---|---|
| `id` | Item ID |
| `score` | Final score used for sorting |
| `recallFrom` | First recall channel that produced the item |
| `recallScore` | Score from `recallFrom`, or `null` if recall did not run |
| `recallFusionScore` | Aggregated multi-channel recall score |
| `rankScore` | Rank-engine contribution, or `null` when ranking was skipped |
| `recallScores` | Insertion-ordered map of every recall channel and its score |

An item recalled by several channels is emitted once while retaining all channel contributions.
`VectorResult` is the separate `id` plus item-sequence-vector carrier used by `item_seq_emb`.

## Compatibility

Changes in this module can affect `sdk/java-client`, `example/init`, `example/web`, and serialized
Kafka or HTTP payloads. Preserve backward parsing where possible and verify those consumers for any
field, enum, or envelope change.

# rec-proto

The wire protocol shared between `rec-server` and its clients. Plain POJOs with Lombok — no Spring,
no transport code — so both sides can depend on it without pulling in a server stack.

Consumed by [rec-client](https://github.com/open-rec/sdk), `example/init` and `rec-server` itself.

```xml
<dependency>
    <groupId>com.openrec</groupId>
    <artifactId>rec-proto</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

It is published to your local Maven repo by `mvn install` at the `rec-server` root, which must
therefore be built before the sdk or the example loader.

## envelope

Every request body is wrapped in `JsonReq<T>`, every response in `JsonRes<T>`.

```java
public class JsonReq<T> {
    private String requestId;   // auto-generated UUID
    private T body;
}

public class JsonRes<T> {
    private int code;           // see ProtoCode
    private boolean status;
    private String msg;
    private T data;
}
```

```json
{"requestId": "b9c1…", "body": {"scene": "scene_0", "size": 10}}
{"code": 200, "status": true, "msg": "", "data": {}}
```

`requestId` is not decoration: `rec-server` pushes it into the SLF4J MDC, so every log line for a
request carries it. Send your own to correlate client and server logs.

`ProtoCode`: `SUCCESS 200`, `BAD_REQUEST 400`, `NOT_FOUND 404`, `ERROR 500`,
`NOT_IMPLEMENTED 501`, `TIMEOUT 504`.

`JsonResType` is a `ParameterizedType` helper that lets a client deserialize `JsonRes<T>` with Gson
despite type erasure — `rec-client` uses it internally.

## data model

`Item`, `User` and `Event` mirror the three tables open-rec ingests. Timestamps are **strings holding
epoch seconds**, and `extFields` is an untyped escape hatch for anything not modelled.

```java
class Item { String id, title, category, tags, scene, pubTime, modifyTime, expireTime;
             int weight, status; Object extFields; }

class User { String id, deviceId, name, gender, phone, country, city, registerTime, loginTime;
             int age; List<String> tags; Object extFields; }

class Event { String userId, deviceId, itemId, traceId, scene, type, value, time;
              boolean isLogin; Object extFields; }
```

Note the asymmetry: `Item.tags` is a comma-separated `String` while `User.tags` is a `List<String>`.

`Event.type` is free-form on the wire; the server-side enum `RecEventType` covers `click`, `expose`,
`collect`, `like`, `comment`, `buy`, `dislike`. `click` drives trigger selection and `expose` drives
exposure filtering, so those two names matter.

Result carriers:

```java
class ScoreResult  { String id; double score; }        // an item plus its score
class VectorResult { String id; List<Double> vector; } // an item plus its embedding
```

## push

`AbstractPushReq<T>` carries a command plus a batch, so one call can insert, update or delete many
rows:

```java
class AbstractPushReq<T> { PushCmd cmd = PushCmd.INSERT; List<T> data; }

enum PushCmd { INSERT, UPDATE, DELETE }
```

`ItemReq`, `UserReq` and `EventReq` are the concrete subclasses. `INSERT` and `UPDATE` behave
identically server-side (an upsert); `DELETE` removes the keys.

```json
{
  "requestId": "1",
  "body": {
    "cmd": "INSERT",
    "data": [{"id": "item-1", "title": "…", "category": "c1", "scene": "s1", "status": 1, "pubTime": "1667355833"}]
  }
}
```

## recommend

```java
class RecommendReq {
    String scene;          // required, partitions all data
    int size;              // how many items to return
    String userId;
    String deviceId;       // for unlogged users
    List<String> itemIds;  // extra triggers, e.g. the item being viewed
    String type;
    boolean debug;         // attach item details to the response
    String targetType;     // endpoint-owned: item or user
}

class RecommendRes<T> {
    List<ScoreResult> results;   // ids + scores, in final order
    List<T> detailInfos;         // populated only when debug = true
}
```

`POST /api/recommend/item` returns `RecommendRes<Item>`. The original `POST /api/recommend` remains
an item-recommendation alias. `POST /api/recommend/user` reserves the future
`RecommendRes<User>` contract but currently returns code 501 without executing a serving graph.

The field **names** of `RecommendReq` are load-bearing: `GraphEngine.prepare()` reflects over them
and exposes each as a DAG parameter under its own name (`scene`, `size`, `userId`, `itemIds`, …), which
nodes read via the constants in `RecParams`. Renaming a field silently detaches it from the nodes
that consume it.

## operate

The blacklist API takes a bare `JsonReq<Set<String>>` of item ids rather than a dedicated request
type.

# rec-server

The online recommendation service of open-rec. A request comes in, a configurable DAG of recall /
filter / rank / operation nodes executes against Redis and Elasticsearch, and a ranked item list
comes back.

Java 8, Spring Boot 2.3.1 with WebFlux, listening on port 13579.

## modules

Directory names differ from artifactIds:

| Directory | artifactId | Contents |
|---|---|---|
| [graph](graph) | `rec-graph` | the generic async DAG engine — no recommendation logic |
| [proto](proto) | `rec-proto` | request/response protocol shared with `rec-client` |
| [contrib](contrib) | `rec-contrib` | pf4j plugin module for custom operation rules |
| [server](server) | `rec-online-server` | the service: nodes, controllers, storage access |

![graph](doc/graph.jpeg "graph")

## build

```shell
mvn clean package -DskipTests
```

Use `mvn clean install -DskipTests` instead if you also need `rec-proto` in your local Maven repo —
[sdk](https://github.com/open-rec/sdk)'s `rec-client` and `example/init` both depend on it.

The runnable jar lands at `server/target/rec-server-1.0-SNAPSHOT.jar` (the `finalName` differs from
the artifactId).

## run

Requires Redis and Elasticsearch. See
[recall-engine](https://github.com/open-rec/recall-engine) for install scripts and the index layout,
and [example_standalone](https://github.com/open-rec/example/tree/master/example_standalone) for a
full walkthrough including sample data.

```shell
cd server
java -jar target/rec-server-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

Any property can be overridden on the command line, which is handy for secrets you do not want in
the properties file:

```shell
java -jar target/rec-server-1.0-SNAPSHOT.jar --spring.profiles.active=dev '--es.password=<your-password>'
```

Optionally install the operation-rule plugin first — `OperationRuleManager` loads it from
`<working-dir>/plugins/`, so it must sit next to wherever you launch the jar:

```shell
mkdir -p server/plugins
cp contrib/target/rec-contrib-1.0-SNAPSHOT.jar server/plugins/
```

Skipping that is fine: the `operation` node logs a warning and passes candidates straight through.

### verify

```shell
curl http://localhost:13579/health

curl -s -X POST http://localhost:13579/api/recommend \
  -H 'Content-Type: application/json' -d '{
  "requestId": "test-1",
  "body": {"scene": "scene_0", "size": 10, "userId": "user_247", "deviceId": "d1", "type": "click", "debug": true}
}'
```

Swagger UI: http://localhost:13579/swagger-ui/index.html

![api](doc/api.png "api doc")

## configuration

`server/src/main/resources/`:

| File | Purpose |
|---|---|
| `application.properties` | selects the active profile |
| `application-dev.properties` | local: pushes data straight to Redis (`pushRedisService`) |
| `application-prod.properties` | pushes data to Kafka (`pushKafkaService`) |
| `graph.json` | the DAG — nodes, their config and the edges between them |
| `logback.xml` | logging |

| Property | Default | Notes |
|---|---|---|
| `server.port` | 13579 | |
| `server.pushService` | `pushRedisService` (dev) | which `PushService` bean the push API uses |
| `redis.hostName` / `redis.port` | 127.0.0.1 / 6379 | |
| `es.host` / `es.port` | 127.0.0.1 / 9200 | Elasticsearch 8 requires `https`; self-signed certs are trusted |
| `es.user` / `es.password` | elastic / — | set your own; ES 8 does not auto-generate one when started with `-d` |
| `rank.host` / `rank.port` | 127.0.0.1 / 8000 | [rank-engine](https://github.com/open-rec/rank-engine), optional |
| `spring.kafka.bootstrap-servers` | localhost:9092 | only used by the `prod` profile |

Standalone setups need neither Kafka nor the rank engine.

## api

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/recommend` | get recommendations |
| POST | `/api/push/{user,item,event}` | ingest data |
| GET | `/api/query/user/{userId}` | read back a user |
| GET | `/api/query/item/{itemId}` | read back an item |
| GET | `/api/query/event/{userId}/{scene}/{type}` | read back a user's events |
| POST | `/api/operate/blacklist` | set the item blacklist |
| GET | `/health` | liveness |

Every request body is wrapped in `JsonReq<T>` and every response in `JsonRes<T>`; see
[proto](proto). [rec-client](https://github.com/open-rec/sdk) is a ready-made Java client.

Set `"debug": true` on a recommend request to get item details attached to the response.

## the request DAG

`graph.json` declares the nodes and edges; `GraphEngine` builds and runs them per request. The
default graph:

```
userTrigger ──> i2i ─────┐
            └──> embedding ─┤
                    hot ─────┤
                    new ─────┼──> combine ──> rank ──> operation ──> collector
                 filter ─────┤        ↑
                  black ─────┘        │
                          userFeature ─┤
                          itemFeature ─┘
```

| Node | Role |
|---|---|
| `userTrigger` | recent clicks from Redis, plus any `itemIds` on the request, used as triggers |
| `i2i` | item-to-item recall from `i2i:{itemId}:{scene}` |
| `embedding` | averages trigger vectors, kNN search in Elasticsearch |
| `hot` / `new` | popularity and freshness recall |
| `filter` | drops items exposed to this user recently |
| `black` | drops blacklisted items (off by default) |
| `combine` | merges and de-duplicates candidates, then removes exposed, blacklisted, triggered, missing, disabled and cross-scene items |
| `itemFeature` | independent item-feature preparation hook reserved for ranking |
| `rank` | scores via [rank-engine](https://github.com/open-rec/rank-engine); failures degrade to recall order |
| `operation` | applies a pf4j `OperationRule` plugin |
| `collector` | truncates to the requested `size`, writes a synthetic expose record |

Each node has a `timeout` in milliseconds. When it is exceeded the engine cancels the node and the
channel silently contributes nothing — a request still succeeds. If a recall channel reports 0 items,
compare its logged latency against its `timeout` before suspecting the data: `embedding`'s default of
100ms is shorter than a cold TLS handshake to Elasticsearch.

To edit the graph, change `graph.json` and repackage — it is read from the classpath. Regenerate the
diagram with `bash server/bin/update_graph.sh` (needs `networkx`, `matplotlib`, graphviz).

Details on writing a node: [graph](graph). Details on custom operation rules: [contrib](contrib).

## test

```shell
mvn -pl graph test                              # standalone JUnit, no services needed
mvn -pl graph test -Dtest=GraphEngineTest       # a single class
mvn -pl server test -Dtest=FilterNodeTest#run   # a single method
```

Tests under `server/src/test` are `@SpringBootTest` and hit **live** Redis / Elasticsearch / Kafka;
they fail without those running, which is why the build commands above use `-DskipTests`.

## function points

<table>
	<tr>
	    <th>Biz</th>
	    <th>Detail</th>
	    <th>Status</th>  
	</tr >
	<tr >
	    <td rowspan="4">recall</td>
	    <td>i2i</td>
	    <td>✅</td>
	</tr>
	<tr>
	    <td>embedding</td>
	    <td>✅</td>
	</tr>
	<tr>
	    <td>hot</td>
	    <td>✅</td>
	</tr>
	<tr>
	    <td>new</td>
	    <td>✅</td>
	</tr>
    <tr >
	    <td rowspan="3">operation</td>
	    <td>blacklist</td>
	    <td>✅</td>
	</tr>
	<tr>
	    <td>exposure filter</td>
	    <td>✅</td>
	</tr>
	<tr>
	    <td>custom operation plugins</td>
	    <td>✅</td>
	</tr>
    <tr>
	    <td>rank</td>
	    <td>LR</td>
        <td>✅</td>
	</tr>
    <tr>
	    <td>tools</td>
	    <td>debug info</td>
        <td>✅</td>
	</tr>
</table>

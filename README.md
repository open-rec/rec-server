# rec-server

[![CI](https://github.com/open-rec/rec-server/actions/workflows/ci.yml/badge.svg)](https://github.com/open-rec/rec-server/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)

`rec-server` is OpenRec's online recommendation service. Item and user recommendation requests
execute independent configurable DAGs that recall, filter, combine, rank, and post-process
candidates before returning the requested entity type.

- Java 21, Spring Boot 4.1.1, and WebFlux
- Default port `13579`
- Redis for online entities, events, and filter state
- Elasticsearch for versioned recall tables and vector indexes

## Modules

| Directory | Artifact | Responsibility |
|---|---|---|
| [`graph`](graph) | `rec-graph` | Recommendation-independent asynchronous DAG runtime |
| [`proto`](proto) | `rec-proto` | Request, response, and entity types shared with clients |
| [`contrib`](contrib) | `rec-contrib` | PF4J operation-rule plugin |
| [`server`](server) | `rec-online-server` | HTTP service, recommendation nodes, and storage access |

The packaged item and user serving graphs are defined independently.

### Item graph

The default item graph is [`item_graph.json`](server/src/main/resources/item_graph.json):

```mermaid
flowchart TD
    user_trigger --> item_cf_i2i
    user_trigger --> content_i2i
    user_trigger --> item_seq_emb

    item_cf_i2i --> combine
    content_i2i --> combine
    user_cf_u2i --> combine
    item_seq_emb --> combine
    hot --> combine
    new --> combine
    filter --> combine
    black --> combine

    combine --> rank
    user_feature --> rank
    item_feature --> rank
    rank --> operation --> collector
```

### User graph

The default user graph is [`user_graph.json`](server/src/main/resources/user_graph.json):

```mermaid
flowchart TD
    user_trigger --> user_cf_u2u
    user_trigger --> content_u2u
    user_trigger --> user_als_emb

    user_cf_u2u --> combine
    content_u2u --> combine
    user_als_emb --> combine
    filter --> combine
    black --> combine

    combine --> rank
    user_feature --> rank
    rank --> operation --> collector
```

## Build and run

Use a JDK 21 installation for Maven and the online server (`JAVA_HOME` must point to it).
The Docker build and runtime stages both use Temurin 21. All modules, including `proto`,
`graph`, and `contrib`, compile with `--release 21`. Their consumers (SDK, example/init and
data-processor) must use Java 21. Spring Boot's BOM is imported only by `server`.

Application JSON uses Jackson 3; Jackson 2 remains an explicit dependency of the Elasticsearch
8.5 transport. Redis values retain their existing JSON representation, without added type metadata.
OpenAPI is available at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`.


```shell
mvn clean package -DskipTests
mkdir -p server/plugins
cp contrib/target/rec-contrib-1.0-SNAPSHOT.jar server/plugins/
(cd server && java -jar target/rec-server-1.0-SNAPSHOT.jar \
  --spring.profiles.active=standalone)
```

Use `mvn clean install -DskipTests` when the Java SDK or example loader needs the local
`rec-proto` artifact.

The default item graph uses the `rec-contrib` plugin. Before launching the jar directly, place it
under the server working directory:

```shell
mkdir -p server/plugins
cp contrib/target/rec-contrib-1.0-SNAPSHOT.jar server/plugins/
```

Alternatively, set an explicit path:

```shell
java -Dopenrec.operation.plugin=/path/to/rec-contrib.jar \
  -jar server/target/rec-server-1.0-SNAPSHOT.jar \
  --spring.profiles.active=standalone
```

The container build includes both the server and plugin. Start the corresponding
`bigdata-platform` mode first, then run one deployment:

```shell
docker compose -f docker-compose.standalone.yml up -d --build --wait
docker compose -f docker-compose.cluster.yml up -d --build --wait
```

- Standalone writes push requests directly to Redis and may bypass rank-engine.
- Cluster publishes push mutations to Kafka, calls rank-engine, and expects clients to report real
  exposures.

The complete initialization and acceptance flows live in `example/example_standalone` and
`example/example_cluster`.

### Recommendation readiness

`/health` reports process liveness; use `/ready` for traffic admission. Recommendation endpoints
return HTTP 503 until their target/experiment graph has been warmed. Push, query and control APIs
remain available so cluster initialization can load data before recommendations become ready.

For the examples, `RECOMMEND_WARMUP_USER_ID` and `RECOMMEND_WARMUP_SCENE` configure an automatic
item recommendation sample on every process start. The startup scripts set these after preparing
the fixture data. A bare server with no sample remains unready; provide representative requests
through the authenticated endpoint after loading your data:

```shell
curl -fsS -X POST http://localhost:13579/internal/recommendation-warmup \
  -H 'Content-Type: application/json' \
  -H 'X-OpenRec-Token: openrec-serving-graph-token-change-me' \
  -d '[{"targetType":"item","scene":"scene_0","userId":"user_0","size":12,"type":"click","params":{"ab":"default","query":"item"}}]'
curl -fsS http://localhost:13579/ready
```

The POST returns 202 while a background worker runs the complete graph with a 10-second request
budget and a 2-second node timeout floor. These apply only to the probe execution. It then requires
three consecutive successful rounds with the original request and node budgets before `/ready`
returns 200. Every probe must produce candidates and finish all nodes successfully; any failed
normal round resets the success count. Built-in collectors suppress synthetic exposures during
both phases using an internal context flag that cannot be supplied by public request parameters.

Provide samples for every target/experiment route you intend to serve, including `targetType=user`
and representative query/vector parameters where used. Uncovered routes remain blocked. Publishing
a new graph invalidates its readiness and automatically rewarms configured samples. The defaults
are `recommend.warmup.deadline-ms=10000`, `node-timeout-ms=2000`, `successes=3` and `max-attempts=20`
(all under `recommend.warmup`). Attempts are one second apart; exhaustion leaves readiness failed.
After fixing dependencies/data, repeat the POST to retry. An empty POST body reuses existing samples.
Custom samples supplied by POST are in-memory; deployment automation must reapply them after restart.

The container healthcheck deliberately uses liveness to avoid a bootstrap dependency cycle.
Configure a load balancer or orchestrator readiness probe against `/ready`. This is a startup gate,
not a promise that later dependency outages or resource contention cannot cause request failures.

## Quick check

Load data and complete the warmup setup above first. `/ready` must return 200 before the
recommendation call below can succeed.

```shell
curl -fsS http://localhost:13579/ready

curl -s -X POST http://localhost:13579/api/recommend/item \
  -H 'Content-Type: application/json' -d '{
  "requestId": "test-1",
  "body": {
    "scene": "scene_0",
    "size": 10,
    "userId": "user_247",
    "deviceId": "d1",
    "type": "click",
    "params": {"queryEmbedding": [0.12, -0.04, 0.31]},
    "debug": true
  }
}'
```

Swagger UI: <http://localhost:13579/swagger-ui/index.html>

## Serving graph and recall

Recall configuration separates graph identity, strategy identity, and storage routing:

| Field | Meaning |
|---|---|
| `name` | Graph node ID; it matches `recallType` for default recall nodes |
| `recallType` | Logical candidate channel used by combine, scoring, and diagnostics |
| `tableName` | Logical recall-store table or index family |

The default item graph enables these channels:

| Channel | Lookup |
|---|---|
| `item_cf_i2i` | `I2iNode`: trigger item to item-CF candidates in `item-cf-i2i` |
| `content_i2i` | `I2iNode`: trigger item to content-similar candidates in `content-i2i` |
| `user_cf_u2i` | `U2iNode`: scene and user to UserCF candidates in `user-cf-u2i` |
| `item_seq_emb` | `EmbeddingNode`: aggregate trigger vectors, then run ANN recall |
| `query_emb` | `QueryEmbeddingNode`: run ANN recall directly from `params.queryEmbedding` |
| `sparse` | `SparseNode`: run BM25 recall from `params.query` against `openrec-recall-sparse-active` |
| `hot` / `new` | Scene-level popular and recent candidates |

The default user graph enables these channels:

| Channel | Lookup |
|---|---|
| `user_cf_u2u` | `U2uNode`: source user to UserCF user candidates in `user-cf-u2u` |
| `content_u2u` | `U2uNode`: source user to content-similar user candidates in `content-u2u` |
| `user_als_emb` | `EmbeddingNode`: read ALS-based U2U results from `user-als-emb` |

Each recall node retains its fixed `@Export` for older graphs and also writes
`recall:<recallType>`. The default `CombineNode` reads the dynamic keys listed in `recallTypes`, so
multiple instances of `I2iNode` can coexist without overwriting the data consumed by combination.

The Elasticsearch store resolves a non-vector `tableName` to a stable active alias:

```text
openrec-recall-{tableName}-active
```

For example, `item-cf-i2i` maps to `openrec-recall-item-cf-i2i-active`. Physical indexes carry a
business date and revision, while queries filter by scene. Embedding recall uses the per-scene
index convention `{scene}-{tableName}-index`. Query and item vectors must have the same dimension
and embedding space. A request without `params.queryEmbedding` produces an empty `query_emb`
channel and leaves the other recall channels unchanged.

Set `recall.store=elasticsearch` or `recall.store=redis` to select the implementation. The Redis
store is primarily a local/debug compatibility path and does not provide atomic activation of
versioned recall indexes.

`rec-console` may publish complete item and user serving graphs through protected internal APIs.
The server validates registered node creation, typed contracts, edge references, and cycles, then atomically switches new
requests of the corresponding target type to the compiled plan. In-flight requests continue with
their existing snapshot.

## Profiles and configuration

Configuration lives in `server/src/main/resources`:

| File | Purpose |
|---|---|
| `application.properties` | Shared defaults and default profile |
| `application-standalone.properties` | Direct Redis push and optional ranking |
| `application-cluster.properties` | Kafka push, rank-engine, and real exposures |
| `item_graph.json` | Packaged default item serving graph |
| `user_graph.json` | Packaged default user serving graph |

Common properties:

| Property | Default |
|---|---|
| `server.port` | `13579` |
| `server.pushService` | `pushRedisService` |
| `redis.hostName` / `redis.port` | `127.0.0.1` / `6379` |
| `es.host` / `es.port` | `127.0.0.1` / `9200` |
| `rank.host` / `rank.port` | `127.0.0.1` / `8123` |
| `serving.graph.item-file` | `item_graph.json` |
| `serving.graph.user-file` | `user_graph.json` |
| `recommend.deadline-ms` | `1000` |

Recommendation graph executions emit a structured `graph_trace` log containing request/experiment/
graph-version metadata and per-node status, queue time, execution time, input/output counts, and
failure class. Aggregate graph and node latency/outcome/volume metrics are available from
`/actuator/prometheus` under the `openrec_graph_*` prefix.

`collector.fake-expose.enabled` is enabled by default in standalone mode, treating returned items
as exposed. It is disabled in cluster mode, where clients report `expose` events after display.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/recommend/item` | Recommend items |
| POST | `/api/recommend` | Backward-compatible item recommendation alias |
| POST | `/api/recommend/user` | Recommend users with the user serving graph |
| POST | `/api/push/{user,item,event}` | Push entities or events |
| GET | `/api/query/user/{userId}` | Query a user |
| GET | `/api/query/item/{itemId}` | Query an item |
| GET | `/api/query/event/{userId}/{scene}/{type}` | Query events |
| POST | `/api/operate/blacklist` | Set the global item blacklist |
| GET | `/health` | Process liveness; does not admit recommendation traffic |
| GET | `/ready` | Recommendation readiness; 200 when ready, otherwise 503 |
| POST | `/internal/recommendation-warmup` | Start warmup/verification; requires `X-OpenRec-Token` |

Requests and responses use `JsonReq<T>` and `JsonRes<T>`; see [`rec-proto`](proto) for the wire
contract. Cluster push messages use a versioned mutation envelope with operation and event time.
Deletes retain tombstones so cumulative offline readers do not resurrect stale entities.

## Test

```shell
mvn -pl graph test
mvn -pl server -am test
mvn -pl server -am -Dtest=RecallStoreUnitTest test
```

Most tests are isolated units. Integration tests such as `EsServiceTest` and `RedisServiceTest`
require their backing services. Tests that construct the default graph also require a loadable
`rec-contrib` plugin because `OperationNode` resolves its configured rule from that jar.

See [`server`](server) for implementation conventions, [`graph`](graph) for node development, and
[`contrib`](contrib) for operation-rule development.

Cluster push success means every message in the request has received a Kafka acknowledgement
(`acks=all`); it does not mean the streaming projections have caught up. The acknowledgement wait
is bounded by `push.kafka.ack-timeout-ms` (default 10000 ms) per message, with producer metadata/buffer
wait bounded separately by `spring.kafka.producer.properties.max.block.ms` (default 10000 ms).
A failed batch can have an acknowledged prefix; a timeout has an unknown delivery outcome.
Clients must retain stable entity IDs and event IDs on retries. Batches are not atomic and the
server does not silently retry an entire batch after a failure. Standalone Redis pushes are unchanged.

### Recall diagnostics for acceptance checks

Recommendation requests with `debug: true` return `data.recallDiagnostics`: one entry per
recall node containing `node`, `channel`, `status`, and `candidateCount`. Counts describe
successfully committed recall outputs before filtering, de-duplication, ranking and top-N
selection. The same item can count toward multiple recall channels. Disabled nodes report
`DISABLED`; failed or timed-out nodes report their execution status and zero candidates.
Normal requests leave diagnostics unset. Existing result attribution (`recallFrom` and
`recallScores`) and recommendation selection are unchanged.

Use these request-scoped diagnostics to verify recall availability. Final results need not
represent every healthy recall channel; channel allocation is a separate operation-rule contract.

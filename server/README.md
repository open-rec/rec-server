# rec-server module

This module contains the WebFlux application, serving DAG nodes, storage adapters, runtime graph
management, and HTTP endpoints. The generic runtime is provided by [`rec-graph`](../graph), shared
wire models by [`rec-proto`](../proto), and operation rules by [`rec-contrib`](../contrib).

Entry point: `com.openrec.RecServer`. Default port: `13579`. Runtime: Java 21 / Spring Boot 4.1.1.

## Package layout

| Package | Responsibility |
|---|---|
| `controller` | Recommend, push, query, operation, health, and serving-graph APIs |
| `graph/node` | Recall, filter, combine, rank, operation, and collector nodes |
| `graph/config` | Typed node configuration deserialized from item/user graph JSON |
| `graph` | Graph loading, request parameter constants, and event types |
| `service/recall` | Elasticsearch recall-store implementation |
| `service` | Entity, event, rank, Kafka, runtime serving, trace, and metrics services |
| `plugin` | PF4J operation-rule loader |
| `config` | Spring configuration for storage, messaging, HTTP clients, and WebFlux |
| `aop` | API logging, MDC correlation, and timing |

## Request flow

Recommendation admission checks the selected target/experiment graph against completed warmup;
unready routes return HTTP 503. For admitted requests, a request-scoped `GraphEngine` executes
the active plan by DAG level. The collector returns ordered `ScoreResult` values. `RecService`
attaches entity details and pre-selection `recallDiagnostics` for debug requests.

The default item graph configures eight recall strategies (some require request-specific input):

| `recallType` / node name | Node | Lookup |
|---|---|---|
| `item_cf_i2i` | `I2iNode` | trigger item to item-CF candidates |
| `content_i2i` | `I2iNode` | trigger item to content-similar candidates |
| `user_cf_u2i` | `U2iNode` | user to collaborative-filter candidates |
| `item_seq_emb` | `EmbeddingNode` | trigger sequence vector to nearest items |
| `query_qwen_emb` | `QueryEmbeddingNode` | supplied vector or online `params.query` embedding to nearest items |
| `item_search` | `SearchNode` | request text to BM25 matches in the active item-search index |
| `hot` | `HotNode` | scene to popular items |
| `new` | `NewNode` | scene to recent items |

Recall configs separate strategy identity from storage routing:

- `recallType` names the logical channel and dynamic context key.
- `tableName` selects the Redis key namespace or Elasticsearch alias/index family.
- `name` identifies the graph node and currently matches `recallType` for recall nodes.

The default user graph is separate: `user_cf_u2u`, `content_u2u`, and the ALS-backed
`user_als_u2u` feed `combine`, then the User Rank model scores candidate users. It deliberately has
no hot/new channels.

Recall nodes publish `recall:<recallType>` dynamically. Fixed annotation exports remain for older
graphs, while the default `CombineNode.recallTypes` configuration consumes dynamic channels. This
allows several instances of the same node class to coexist without overwriting the data actually
used by the current graph.

## Runtime conventions

- Nodes are created through the explicit `NodeRegistry`. The serving registry supports stable
  logical types and registered legacy class-name aliases, then applies normal Spring dependency
  injection. There is no `BeanUtil` bridge.
- `item_graph.json` and `user_graph.json` are the packaged defaults selected by request
  `targetType`. Runtime graph APIs validate and activate versioned graph definitions independently
  without mutating those resource files.
- `redisTemplate` stores string/sorted-set data; `redisJsonTemplate` stores JSON entity values.
- The active profile chooses the push path: standalone writes serving state directly, while cluster
  publishes versioned Kafka mutations for downstream processors.
- `ApiDecorator` logs controller requests, responses, elapsed time, and the request ID in MDC.
- Recommendation requests explicitly propagate request ID, target, scene, experiment, and graph
  version into `GraphTraceContext`; this does not depend on MDC surviving thread switches.
- Operation rules require the PF4J jar described in [`rec-contrib`](../contrib).

## Graph trace and metrics

Each recommendation writes one structured `graph_trace` log. It includes graph metadata and every
node's status, queue/execution time, input/output count, and sanitized failure type. Raw request
payloads, feature values, and exception messages are not included.

The Prometheus actuator exposes these graph metrics:

- `openrec_graph_executions`
- `openrec_graph_latency`
- `openrec_graph_node_executions`
- `openrec_graph_node_latency`
- `openrec_graph_node_queue_latency`
- `openrec_graph_node_input_items`
- `openrec_graph_node_output_items`

Metric labels are bounded to target type, experiment, registered node type, status, and deadline
outcome. Request IDs, scenes, graph versions, and entity IDs are trace fields rather than metric
labels.

## Run and inspect

Build from the repository root, then launch with the desired profile:

```shell
mvn clean package -DskipTests
java -Dopenrec.operation.plugin="$PWD/contrib/target/rec-contrib-0.1.0.jar" \
  -jar server/target/rec-server-0.1.0.jar \
  --spring.profiles.active=standalone
```

After loading serving data, configure `RECOMMEND_WARMUP_USER_ID` before startup or submit
representative samples to `/internal/recommendation-warmup`. Wait for `/ready` to return 200
before sending recommendations; `/health` and Docker healthchecks indicate liveness only.
The [root readiness guide](../README.md#recommendation-readiness) describes token configuration,
independent warmup budgets and normal-budget verification. Example startup scripts automate this.

Swagger UI is available at <http://localhost:13579/swagger-ui/index.html>. See the repository
[`README`](../README.md) for dependencies, Docker usage, configuration, and endpoint examples.

## Test

```shell
mvn -pl server -am test
mvn -pl server -am test -Dtest=FilterNodeTest#run
```

Most node and service tests are isolated unit tests. Storage, messaging, or full application tests
may require Redis, Elasticsearch, Kafka, the operation plugin, or other profile-specific services;
check the selected test before assuming it is self-contained.

Full-text recall is implemented by `SearchNode` with `SearchConfig` and registry type
`item.search`. Its channel is `item_search` and table is `item-search`, and the request parameter
remains `query` by default. It exports `searchItems` and the configured recall channel.
Persisted graph overrides must replace the former sparse node/config class names with
`com.openrec.graph.node.item.SearchNode` / `com.openrec.graph.config.SearchConfig`
and use `item.search` if specifying a registry type. The former empty SearchNode now
requires recall content configuration; the packaged item graph supplies it.

User full-text recall uses `user.search` with `SearchConfig`, channel `user_search`,
table `user-search`, and alias `openrec-recall-user-search-active`. Both searches read
`params.query`; absent or blank queries skip retrieval.


Online query embeddings are optional (`EMBEDDING_ENABLED=false` by default).
`EMBEDDING_ENDPOINT` is a full OpenAI-compatible `/v1/embeddings` URL; the default is
`http://vllm:8000/v1/embeddings`. Configure `EMBEDDING_MODEL` (default
`Qwen/Qwen3-Embedding-0.6B`), `EMBEDDING_API_KEY`, `EMBEDDING_DIMENSIONS` (1024),
`EMBEDDING_TIMEOUT_MS` (500), `EMBEDDING_MAX_CONCURRENT` (8), and
`EMBEDDING_MAX_CHARS` (4096). `OPENREC_TEXT_EMBEDDING_INSTRUCTION` is the Qwen
query instruction, shared with the offline encoder; set it empty for models that
require plain query text. Requests contain model IDs, never local model paths.

`QueryEmbeddingNode` prefers explicit `params.queryEmbedding`, otherwise encodes
`params.query`. Remote responses must have the expected dimensions and finite,
nonzero values; vectors are L2-normalized. Timeout, saturation, malformed output,
or provider failure skips this channel. The packaged node has an 800 ms budget;
inference and Elasticsearch share that budget, within the overall request deadline.
When changing timeouts, synchronize persisted graph overrides and the request deadline.

Trigger nodes export a single immutable `requestTrigger`: `triggerItems` (item ID/score seeds), `triggerUsers` (user click ID/score seeds),
`triggerSelf` (user ID), and `triggerQuery` (text). Recall and combine nodes consume
this object; custom nodes must migrate former `triggerItems`/`triggerUsers` imports
to `requestTrigger`. Empty user/history seeds leave text and query-vector recall available.

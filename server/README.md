# rec-server module

This module contains the WebFlux application, serving DAG nodes, storage adapters, runtime graph
management, and HTTP endpoints. The generic runtime is provided by [`rec-graph`](../graph), shared
wire models by [`rec-proto`](../proto), and operation rules by [`rec-contrib`](../contrib).

Entry point: `com.openrec.RecServer`. Default port: `13579`.

## Package layout

| Package | Responsibility |
|---|---|
| `controller` | Recommend, push, query, operation, health, and serving-graph APIs |
| `graph/node` | Recall, filter, combine, rank, operation, and collector nodes |
| `graph/config` | Typed node configuration deserialized from `graph.json` |
| `graph` | Graph loading, request parameter constants, and event types |
| `service/recall` | Redis and Elasticsearch recall-store implementations |
| `service` | Entity, event, rank, Kafka, and runtime serving services |
| `plugin` | PF4J operation-rule loader |
| `config` | Spring configuration for storage, messaging, HTTP clients, and WebFlux |
| `aop` | API logging, MDC correlation, and timing |

## Request flow

For every recommendation request, the active graph plan is selected, a request-scoped
`GraphEngine` is created, request fields are added to its context, and ready nodes execute by DAG
level. The collector returns ordered `ScoreResult` values and optionally loads item details for
debug responses.

The default graph combines six recall strategies:

| `recallType` / node name | Node | Lookup |
|---|---|---|
| `item_cf_i2i` | `I2iNode` | trigger item to item-CF candidates |
| `content_i2i` | `I2iNode` | trigger item to content-similar candidates |
| `user_cf_u2i` | `U2iNode` | user to collaborative-filter candidates |
| `item_seq_emb` | `EmbeddingNode` | trigger sequence vector to nearest items |
| `hot` | `HotNode` | scene to popular items |
| `new` | `NewNode` | scene to recent items |

Recall configs separate strategy identity from storage routing:

- `recallType` names the logical channel and dynamic context key.
- `tableName` selects the Redis key namespace or Elasticsearch alias/index family.
- `name` identifies the graph node and currently matches `recallType` for recall nodes.

Recall nodes publish `recall:<recallType>` dynamically. Fixed annotation exports remain for older
graphs, while the default `CombineNode.recallTypes` configuration consumes dynamic channels. This
allows several instances of the same node class to coexist without overwriting the data actually
used by the current graph.

## Runtime conventions

- Nodes are reflectively constructed, not managed by Spring. They access services through
  `BeanUtil`.
- `graph.json` is the packaged default. Runtime graph APIs validate and activate versioned graph
  definitions without mutating that resource file.
- `redisTemplate` stores string/sorted-set data; `redisJsonTemplate` stores JSON entity values.
- The active profile chooses the push path: standalone writes serving state directly, while cluster
  publishes versioned Kafka mutations for downstream processors.
- `ApiDecorator` logs controller requests, responses, elapsed time, and the request ID in MDC.
- Operation rules require the PF4J jar described in [`rec-contrib`](../contrib).

## Run and inspect

Build from the repository root, then launch with the desired profile:

```shell
mvn clean package -DskipTests
java -jar server/target/rec-server-1.0-SNAPSHOT.jar \
  --spring.profiles.active=standalone
```

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

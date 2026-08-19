# rec-online-server

The service module: HTTP layer, DAG nodes, and storage access. Everything domain-specific lives here;
the DAG machinery itself is in [rec-graph](../graph) and the wire types in [rec-proto](../proto).

Entry point: `com.openrec.RecServer`. Port 13579.

## layout

| Package | Contents |
|---|---|
| `controller/` | WebFlux endpoints — recommend, push, query, operate, health |
| `graph/node/` | the DAG nodes (`I2iNode`, `EmbeddingNode`, `RankNode`, `CollectorNode`, …) |
| `graph/config/` | typed config for each node, deserialized from `graph.json` |
| `graph/` | `RecTemplate` (loads `graph.json`), `RecParams`, `RecEventType` |
| `service/` | `RedisService`, `EsService`, `RankService`, `KafkaService`, push/query/operate |
| `plugin/` | `OperationRuleManager` — pf4j loader for [rec-contrib](../contrib) |
| `config/` | Spring beans: Redis, Elasticsearch, Kafka, WebFlux, RestTemplate |
| `aop/` | `ApiDecorator` (request/response logging + MDC), `@TimeCost` |
| `util/` | `BeanUtil`, `JsonUtil`, `FileUtil`, `TimeUtil` |

## run

From this directory, after `mvn clean package -DskipTests` at the repo root:

```shell
java -jar target/rec-server-1.0-SNAPSHOT.jar --spring.profiles.active=standalone
```

Launch from here (not the repo root) if you use the operation-rule plugin — it is resolved as
`<working-dir>/plugins/rec-contrib-1.0-SNAPSHOT.jar`.

Needs Redis and Elasticsearch; the rank engine and Kafka are optional. Full setup, sample data and
verification steps:
[example_standalone](https://github.com/open-rec/example/tree/master/example_standalone).

## api document

http://localhost:13579/swagger-ui/index.html

![api](../doc/api.png "api doc")

## conventions worth knowing

**Nodes are not Spring beans.** `GraphEngine` instantiates them reflectively per request, so
`@Autowired` fields stay null. Get collaborators through `BeanUtil.getBean(RedisService.class)`.

**`graph.json` is read from the classpath.** `RecTemplate` hashes it to support hot reload, but
`RecService` caches the parsed config in its constructor — in practice, editing the graph means
repackage and restart.

**Two Redis templates exist.** `redisTemplate` uses String serializers (used for sorted sets),
`redisJsonTemplate` uses `GenericJackson2JsonRedisSerializer` (used for user/item JSON values). Check
which one a `RedisService` method uses before adding another.

**Push implementation is profile-driven.** `PushController` injects by the `server.pushService`
property: `pushRedisService` writes straight to Redis (dev), `pushKafkaService` publishes to the
`item` / `user` / `event` topics (prod).

**Logging.** `ApiDecorator` wraps every `*Controller` method to log the request, response and elapsed
time, and puts `JsonReq.requestId` into the SLF4J MDC so all lines for a request are correlated. Add
`@TimeCost` to any method for timing.

## tests

```shell
mvn -pl server test
mvn -pl server test -Dtest=FilterNodeTest#run
```

These are `@SpringBootTest` and talk to **live** Redis / Elasticsearch / Kafka — start those first, or
build with `-DskipTests`. `EsServiceTest` and `RedisServiceTest` in particular write real keys and
indexes, and the credentials come from `application-standalone.properties`, so update `es.password` before
running them.

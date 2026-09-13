# rec-graph

`rec-graph` is a lightweight asynchronous DAG runtime. It has no recommendation or Spring
dependencies: it compiles a graph definition, runs ready nodes concurrently, and exchanges data
through a request-scoped context. Recommendation-specific nodes live in `rec-server`.

## Use the engine

Compile reusable graph metadata once, then create one engine per request:

```java
NodeRegistry registry = NodeRegistry.builder()
    .register(new SimpleNodeFactory("recall", RecallNode::new))
    .build();
GraphPlan plan = GraphPlan.compile(graphConfig, registry);

GraphEngine engine = GraphEngine.getSessionGraphEngine();
engine.prepare(requestObject);       // declared fields become graph parameters
engine.execGraph(plan);
List<MyResult> result = engine.getResult();
```

`GraphEngine` is stateful and must not be shared between requests. `GraphPlan` is immutable and may
be reused. The older `buildGraph(...)` followed by `execGraph()` API remains available.

`destroy()` clears only request-scoped state. Worker and timeout executors are bounded, daemonized,
and shared by all engine instances.

## Graph definition

```json
{
  "nodes": [
    {
      "name": "hot",
      "type": "item.hot",
      "configClazz": "com.openrec.graph.config.HotConfig",
      "open": true,
      "timeout": 100,
      "content": {"size": 1000}
    }
  ],
  "edges": [
    {"from": "hot", "to": "combine"}
  ]
}
```

| Field | Purpose |
|---|---|
| `name` | Unique node ID used by edges and the context config map |
| `type` | Stable node type registered by the application |
| `clazz` | Deprecated implementation-class alias for existing graphs |
| `configClazz` | Type used to deserialize `content`; may be `null` |
| `open` | Node-level feature switch interpreted by the node |
| `timeout` | Execution timeout in milliseconds |
| `content` | Node-specific configuration |

`GraphPlan.compile(...)` validates registered node types and edges, resolves each node's
factory, and precomputes dependency metadata. Unknown types and invalid edge references
fail before request execution. In an acyclic definition, nodes with no incoming edges are roots.
Cycle, duplicate-node, duplicate-edge, endpoint, timeout, node-factory, annotated-field, and typed
data-contract validation all happen in `GraphPlan.compile(...)`.

## Write a node

Most server nodes extend `AbstractSyncNode<C>`:

```java
public class HotNode extends AbstractSyncNode<HotConfig> {

    @Import("triggerItems")
    private List<ScoreResult> triggerItems;

    @Export("hotItems")
    private List<ScoreResult> hotItems = Lists.newArrayList();

    public HotNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen()) {
            return;
        }
        String scene = context.getParams().getValueToString("scene");
        // Populate hotItems.
    }
}
```

Nodes are created by their registered factory. In `rec-server`, the registry uses explicit
constructors and Spring autowiring, so node dependencies use normal `@Autowired` injection. Legacy
graphs containing `clazz` remain supported through registered class-name aliases.

### Data exchange

The field annotation API is the primary concise contract for server nodes. Before a node runs,
fields annotated with `@Import("key")` are populated from `GraphContext`. After it finishes,
`@Export("key")` fields are published. `GraphPlan.compile(...)` reflects these fields once and
validates that every required import has a reachable upstream export with a compatible Java type,
including generic arguments such as `List<ScoreResult>`. Duplicate or blank ports are rejected.
The compiled contract is reused for request execution, so fields are not rescanned for every node
invocation.

Imports are required by default. Use `@Import(value = "key", required = false)` only when a node
explicitly handles a missing value. A missing required value at runtime fails that node instead of
silently injecting `null`.

`TypedNode` remains available for nodes that benefit from immutable `NodeInput`/`NodeOutput`
snapshots. Such nodes declare `DataKey<T>` inputs and outputs, and the engine commits their complete
output only after the node reaches `SUCCESS`.

Nodes may also call `context.addData(key, value)` and `context.getData(key)` for keys chosen at
runtime. `rec-server` uses this for configurable recall channels while retaining fixed annotation
exports for backward compatibility. A later write to the same key replaces the earlier value.

Initialize exported collections when an open node can legitimately produce no values. A failed,
skipped, or timed-out producer does not publish output, so a required downstream import fails
explicitly according to the graph's failure policy.

### Request parameters and result

`prepare(object)` reflects over the object's declared fields and stores each value under its field
name. Parameters can also be added explicitly:

```java
engine.addParam("scene", "home");
String scene = context.getParams().getValueToString("scene");
```

The terminal node calls `context.setResult(value)`; the caller retrieves it with
`engine.getResult()`.

## Execution and timeout behavior

The runtime executes one dependency level at a time. All ready nodes in a level run concurrently,
then the next level starts after the batch completes. Every node has a timeout and the whole request
may have an earlier deadline. A timed-out or cancelled invocation is interrupted and its late output
is never committed. `failurePolicy` controls whether failure continues, skips descendants, or fails
the graph.

## Execution trace

Callers may supply a `GraphTraceContext` and `GraphTraceObserver` when creating an engine. The
observer receives one immutable `GraphExecutionTrace` when execution finishes, including request,
target, scene, experiment and graph-version metadata. Every node entry contains its name, type,
terminal status, queue and execution durations, input/output counts, and a sanitized failure class.

```java
GraphEngine engine = GraphEngine.getSessionGraphEngine(traceContext, observer);
engine.execGraph(plan, 1000L);
GraphExecutionTrace trace = engine.getTrace();
```

Trace context is passed explicitly instead of relying on thread-local state, so it remains correct
when WebFlux dispatches work to the blocking and graph worker pools.

## Test

```shell
mvn -pl graph test
```

The graph module tests are self-contained and require no external services.

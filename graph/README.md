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
Cycle, duplicate-node, duplicate-edge, endpoint, timeout, node-factory, and typed data-contract
validation all happen in `GraphPlan.compile(...)`.

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

New nodes should implement `TypedNode`, declare `DataKey<T>` inputs and outputs, and return an
immutable `NodeOutput`. The engine gives each invocation an immutable `NodeInput` snapshot and
commits the complete output only after the node reaches `SUCCESS`. `GraphPlan.compile(...)` rejects
missing inputs and duplicate typed outputs when the path is fully typed.

The annotation API remains as a migration adapter for existing nodes:

Before a node runs, fields annotated with `@Import("key")` are populated from `GraphContext`.
After it finishes, `@Export("key")` fields are published. The key is the contract, so producers and
consumers must use exactly the same value.

Nodes may also call `context.addData(key, value)` and `context.getData(key)` for keys chosen at
runtime. `rec-server` uses this for configurable recall channels while retaining fixed annotation
exports for backward compatibility. A later write to the same key replaces the earlier value.

Initialize exported collections even when a node can be disabled or time out. Otherwise consumers
may receive `null`.

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

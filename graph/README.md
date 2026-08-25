# rec-graph

`rec-graph` is a lightweight asynchronous DAG runtime. It has no recommendation or Spring
dependencies: it compiles a graph definition, runs ready nodes concurrently, and exchanges data
through a request-scoped context. Recommendation-specific nodes live in `rec-server`.

## Use the engine

Compile reusable graph metadata once, then create one engine per request:

```java
GraphPlan plan = GraphPlan.compile(graphConfig);

GraphEngine engine = GraphEngine.getSessionGraphEngine();
engine.prepare(requestObject);       // declared fields become graph parameters
engine.execGraph(plan);
List<MyResult> result = engine.getResult();
```

`GraphEngine` is stateful and must not be shared between requests. `GraphPlan` is immutable and may
be reused. The older `buildGraph(...)` followed by `execGraph()` API remains available.

Do not call `destroy()` during normal request handling: it shuts down the static executor pools
shared by all engine instances.

## Graph definition

```json
{
  "nodes": [
    {
      "name": "hot",
      "clazz": "com.openrec.graph.node.HotNode",
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
| `clazz` | Node implementation class |
| `configClazz` | Type used to deserialize `content`; may be `null` |
| `open` | Node-level feature switch interpreted by the node |
| `timeout` | Execution timeout in milliseconds |
| `content` | Node-specific configuration |

`GraphPlan.compile(...)` validates node classes and edges, resolves each node's
`NodeConfig` constructor, and precomputes dependency metadata. Invalid classes and edge references
fail before request execution. In an acyclic definition, nodes with no incoming edges are roots.
Applications accepting runtime graph updates should also validate cycles, as `rec-server` does.

## Write a node

Most server nodes extend `SyncNode<C>`:

```java
public class HotNode extends SyncNode<HotConfig> {

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

Nodes are instantiated reflectively, so each implementation needs a constructor accepting one
`NodeConfig`. They are not Spring beans; server nodes obtain application services through the
server's `BeanUtil` bridge.

### Data exchange

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
then the next level starts after the batch completes. A timeout interrupts the node task and graph
execution continues, so nodes should tolerate interruption and publish safe empty output when they
cannot complete.

## Test

```shell
mvn -pl graph test
```

The graph module tests are self-contained and require no external services.

# rec-graph

A small async DAG engine. It knows nothing about recommendation — it loads a node graph from config,
runs nodes as their dependencies complete, and moves data between them through a shared context.
`rec-server`'s recall / rank / operation nodes are built on it.

Dependencies: Guava, Gson, commons-lang3, Lombok, slf4j. No Spring.

## usage

```java
GraphEngine engine = GraphEngine.getSessionGraphEngine();  // one instance per request
engine.prepare(requestObject);                             // request fields -> params
engine.buildGraph(graphConfig);
engine.execGraph();
List<MyResult> result = engine.getResult();
```

`GraphEngine` is stateful and single-use: it holds the queue, the visited set and the context for one
execution. Do not share one across requests.

> `destroy()` shuts down the **static** thread pools shared by every engine instance, killing
> execution for all in-flight requests. `rec-server` deliberately never calls it.

## config

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

| Field | Meaning |
|---|---|
| `name` | node id, referenced by `edges`; also the key in the context's config map |
| `clazz` | implementation class, instantiated **reflectively** |
| `configClazz` | type that `content` is deserialized into; `null` for nodes without config |
| `open` | feature switch — see below, the node still runs |
| `timeout` | milliseconds before the engine cancels the node |
| `content` | node-specific config, typed by `configClazz` |

Nodes are created with `Class.forName(clazz).getDeclaredConstructor(NodeConfig.class)`, so **every
node needs a public constructor taking a single `NodeConfig`**. A class that fails to instantiate is
logged and skipped, leaving its consumers to fail later.

A node listed in `nodes` but absent from `edges` never executes. Nodes that appear only as an
edge `from` (no parents) become roots and start immediately.

## writing a node

Extend `SyncNode<C>`, where `C` is the config content type (`Void` if the node has none):

```java
public class HotNode extends SyncNode<HotConfig> {

    @Import("triggerItems")
    private List<ScoreResult> triggerItems;      // filled in before run()

    @Export("hotItems")
    private List<ScoreResult> hotItems;          // published after run()

    public HotNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.hotItems = Lists.newArrayList();    // initialize! see below
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen()) {
            return;                              // honour the switch yourself
        }
        String scene = context.getParams().getValueToString("scene");
        int size = config.getContent().getSize();
        hotItems = ...;
    }
}
```

`AsyncNode` exists for non-blocking implementations (`buildQuery` / `handleResult`) but the shipped
nodes are all `SyncNode`; the engine already runs each node on its own thread.

### data flow is by annotation, not method call

Nodes never reference each other. After a node runs, every `@Export("key")` field is copied into the
context's data map; before a node runs, every `@Import("key")` field is populated from it. **The
string keys are the entire contract between nodes** — a typo compiles fine and silently yields null.

Consequences worth knowing:

- **Initialize exported collections in the constructor.** A node with `open: false`, or one cancelled by its timeout, still gets its exports published — whatever the field holds at that moment. An uninitialized field publishes `null` and NPEs its consumer.
- **A missing producer is not a graceful failure.** If the node that exports a key is not in the graph, the importing node's field stays null.

### reading request params

`prepare(obj)` reflects over the declared fields of the object you pass and puts them into
`GraphParams` **keyed by field name**. Typed getters:

```java
context.getParams().getValueToString("scene");
context.getParams().getValueToInt("size");
context.getParams().getValueToList("itemIds");
```

Adding a field to the request object makes it available automatically. `getValueTo*` return
zero-values (`""`, `0`, `false`) for absent keys, but the collection getters return `null`.

### obtaining collaborators

Nodes are constructed reflectively, not by Spring, so `@Autowired` does **not** work in them.
`rec-server` reaches its services through a static accessor:

```java
private RedisService redisService = BeanUtil.getBean(RedisService.class);
```

## execution model

Despite the name, execution is level-synchronized rather than fully pipelined:

1. collect every node whose parents have all finished
2. submit that batch to a shared pool, one task per node
3. `CountDownLatch.await()` the whole batch
4. repeat until the queue drains

So a slow node in a batch delays the entire next level, even nodes that do not depend on it.

Each node is also handed to a watchdog pool that calls `future.cancel(true)` after `timeout` ms.

### timeouts degrade silently

A cancelled node's thread is interrupted; the resulting `InterruptedException` surfaces inside the
node, and nodes typically catch and log it. Execution continues, the node publishes whatever its
fields hold, and the request succeeds minus that node's contribution. There is no signal in the
response that a node was dropped.

Set `timeout` generously for anything doing I/O on a cold JVM: a first request paying for a TLS
handshake can take several hundred milliseconds where steady state is tens.

## returning a result

The terminal node sets the result on the context; `getResult()` casts it for the caller:

```java
context.setResult(finalItems);
```

## testing

`GraphEngineTest` builds a three-node graph out of `SleepNode` and asserts total time is below the
serial sum, i.e. that independent nodes really do run concurrently. `EmptyNode`, `SleepNode` and
`RootNode` are there for tests and as the graph's synthetic entry point.

```shell
mvn -pl graph test
```

These tests need no external services.

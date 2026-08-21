# rec-contrib

Extension module for business-specific rules that do not belong in the core server. Built as a
[pf4j](https://github.com/pf4j/pf4j) plugin, so rules can be swapped by replacing a jar and
restarting — no rebuild of `rec-server`.

Only one extension point exists today: `OperationRule`, applied by the DAG's `operation` node after
ranking.

## the extension point

```java
public interface OperationRule extends ExtensionPoint {
    List<ScoreResult> handle(GraphContext context, List<ScoreResult> inputItems);
}
```

It receives the ranked candidates and returns the list to hand downstream. That means a rule can
reorder, drop, or inject items — pinning editorial picks to the top, enforcing category diversity,
suppressing a campaign, and so on. The `GraphContext` gives access to the request params (`scene`,
`userId`, …) and to any data earlier nodes exported.

The shipped rules are:

- `DefaultOperationRule`: no-op reference.
- `WeightedChannelOperationRule`: allocates the requested size by channel ratio, selects the
  highest-scoring items inside each quota, fills shortages with the highest remaining candidates,
  and sorts the selected result by score.
- `RandomInsertOperationRule`: reserves the highest-scoring candidates from configured channels and
  inserts them at random final positions.
- `SlidingWindowDiversityOperationRule`: keeps weighted channel selection as its preferred order,
  then reorders/fills candidates so configured item attributes repeat at most `repeatK` times in
  every `windowSize` results.

Channel assignment uses `ScoreResult.recallFrom`, the first channel that recalled a de-duplicated
item. Secondary hits in `recallScores` remain visible but do not consume several channel quotas.

The no-op reference is:

```java
@Extension
public class DefaultOperationRule implements OperationRule {
    @Override
    public List<ScoreResult> handle(GraphContext context, List<ScoreResult> inputItems) {
        return inputItems;
    }
}
```

## configuring the bundled rules

Weighted allocation:

```json
"content": {
  "operationName": "WeightedChannelOperationRule",
  "channelRatios": {
    "i2i": 0.3,
    "embedding": 0.3,
    "hot": 0.2,
    "new": 0.2
  }
}
```

Ratios are normalized if they do not sum to 1. Largest-remainder rounding makes quotas add up to
the requested size. If one channel has too few candidates, the highest-scoring unused candidates
from other channels fill the shortage.

Random guaranteed insertion:

```json
"content": {
  "operationName": "RandomInsertOperationRule",
  "randomInsertRatios": {
    "hot": 0.1,
    "new": 0.1
  }
}
```

Each configured channel reserves `ceil(size × ratio)` positions when enough candidates exist.
Candidate selection within the channel remains score-first; only final positions are random. Other
channels fill all ordinary positions, keeping the configured share exact when supply permits. If
either side is short, the rule fills the result from any remaining channel rather than inventing
candidates.

Sliding-window diversity (the default graph rule):

```json
"content": {
  "operationName": "SlidingWindowDiversityOperationRule",
  "windowSize": 5,
  "repeatK": 2,
  "diversityDimensions": ["category", "tags"],
  "channelRatios": {
    "i2i": 0.3,
    "embedding": 0.3,
    "hot": 0.2,
    "new": 0.2
  }
}
```

`category` constrains equal categories. `tags` treats every tag on an item as a key, so none of its
tags may exceed the limit. Multiple entries such as `["category", "tags"]` enforce both dimensions
independently. Use `["category+tags"]` to constrain category/tag pairs instead; an item with three
tags contributes three pair keys. Expressions can also be mixed.

The rule selects the first eligible candidate from the preferred channel-balanced order, then
considers unused candidates by score. The limit is strict: if the pool cannot produce the requested
number without breaking the constraint, it returns fewer items. An empty dimension list or a
non-positive window/repeat value disables diversity.

## writing a rule

1. Add a class in `com.openrec.contrib.operation.impl` implementing `OperationRule`, annotated `@Extension`.
2. Rebuild: `mvn -pl contrib package`.
3. Select it by **simple class name** in `graph.json`:

```json
{
  "name": "operation",
  "clazz": "com.openrec.graph.node.OperationNode",
  "configClazz": "com.openrec.graph.config.OperationConfig",
  "open": true,
  "timeout": 20,
  "content": {"operationName": "MyOperationRule"}
}
```

`OperationRuleManager` indexes extensions by `getClass().getSimpleName()`, so `operationName` must
match the class name exactly — not the fully qualified name.

Keep the work inside the node's `timeout` (20ms by default). Exceeding it gets the node cancelled and
the rule's effect silently dropped.

## deployment

The jar is **not** loaded from the classpath. By default, `OperationRuleManager` looks relative to
the server's working directory:

```
<working-dir>/plugins/rec-contrib-1.0-SNAPSHOT.jar
```

So after building, copy it next to wherever you launch the server jar:

```shell
mvn -pl contrib package
mkdir -p server/plugins
cp contrib/target/rec-contrib-1.0-SNAPSHOT.jar server/plugins/
cd server && java -jar target/rec-server-1.0-SNAPSHOT.jar --spring.profiles.active=standalone
```

pf4j identifies the plugin through manifest entries injected by `maven-jar-plugin`:

| Entry | Value |
|---|---|
| `Plugin-Id` | `contrib-plugins` |
| `Plugin-Version` | `1.0-SNAPSHOT` |

Renaming the artifact or bumping its version changes the path `OperationRuleManager` expects, so keep
them in sync.

Override the path when the plugin is built elsewhere. The standalone launcher uses this option for
its isolated build directory:

```shell
java -Dopenrec.operation.plugin=/absolute/path/rec-contrib-1.0-SNAPSHOT.jar -jar ...
```

## when the plugin is missing

Loading failures are not fatal. `OperationRuleManager` logs
`load jar file:… failed` at startup, `getOperationRuleByName` returns null, and `OperationNode` warns
`operation load <name> failed, please check again` and passes candidates through untouched. The
service keeps serving recommendations — so an absent or stale plugin shows up only in the logs, not in
the response.

## dependencies

`rec-proto` (for `ScoreResult`), `rec-graph` (for `GraphContext`) and pf4j. Deliberately no Spring:
plugin classes are instantiated by pf4j, not the application context, so `@Autowired` does not work
here. Reach server beans through `BeanUtil.getBean(...)` if a rule genuinely needs one.

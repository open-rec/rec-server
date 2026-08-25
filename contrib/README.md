# rec-contrib

`rec-contrib` packages business-specific post-ranking rules as a PF4J plugin. Rules can reorder,
remove, or inject candidates without adding business policy to the core server.

The current extension point is `OperationRule`:

```java
public interface OperationRule extends ExtensionPoint {
    List<ScoreResult> handle(GraphContext context, List<ScoreResult> inputItems);
}
```

The context exposes request parameters and upstream graph data. Channel-aware rules use
`ScoreResult.recallFrom`; the complete multi-channel provenance remains available through
`recallScores`.

## Bundled rules

| Rule | Behavior |
|---|---|
| `DefaultOperationRule` | Returns candidates unchanged |
| `WeightedChannelOperationRule` | Allocates result slots by recall-channel ratio and fills shortages by score |
| `RandomInsertOperationRule` | Reserves candidates from selected channels and inserts them at random positions |
| `SlidingWindowDiversityOperationRule` | Applies channel balancing plus sliding-window attribute constraints |

The default serving graph selects `WeightedChannelOperationRule` with these current channels:

```json
{
  "operationName": "WeightedChannelOperationRule",
  "channelRatios": {
    "item_cf_i2i": 0.2,
    "content_i2i": 0.1,
    "user_cf_u2i": 0.1,
    "item_seq_emb": 0.2,
    "hot": 0.2,
    "new": 0.2
  }
}
```

Ratios are normalized when they do not total one. Largest-remainder rounding makes quotas add up
to the requested size, and unused high-score candidates fill undersupplied channels.

Random insertion uses `randomInsertRatios`, for example `{"hot": 0.1, "new": 0.1}`. Sliding-window
diversity additionally accepts:

```json
{
  "windowSize": 5,
  "repeatK": 2,
  "diversityDimensions": ["category", "tags"]
}
```

Each configured value may appear at most `repeatK` times in a window. `category+tags` constrains
combined keys. When the available pool cannot satisfy a strict constraint, the rule may return
fewer results.

## Add a rule

1. Implement `OperationRule` under `com.openrec.contrib.operation.impl`.
2. Annotate the class with `@Extension`.
3. Build the plugin with `mvn -pl contrib package`.
4. Set `operationName` in `graph.json` to the implementation's simple class name.

`OperationRuleManager` indexes extensions by simple class name, so a fully qualified name will not
match. Keep rule execution within the operation node's timeout.

## Load the plugin

The plugin is loaded as a jar rather than from the application classpath. The default path is:

```text
<server-working-directory>/plugins/rec-contrib-1.0-SNAPSHOT.jar
```

Build and stage it with:

```shell
mvn -pl contrib package
mkdir -p server/plugins
cp contrib/target/rec-contrib-1.0-SNAPSHOT.jar server/plugins/
```

Override the location when necessary:

```shell
java -Dopenrec.operation.plugin=/absolute/path/rec-contrib-1.0-SNAPSHOT.jar \
  -jar server/target/rec-server-1.0-SNAPSHOT.jar
```

The manifest declares plugin ID `contrib-plugins`. Keep the artifact path, version, and loader
expectation synchronized when changing packaging.

If the jar fails to load, the manager logs the error and the configured rule is unavailable. A
graph containing `OperationNode` with that rule then fails node construction/validation; it is not
silently replaced by a pass-through rule.

## Dependencies

The plugin depends on `rec-proto`, `rec-graph`, and PF4J, but deliberately not Spring. Plugin
extensions are created by PF4J, so Spring injection is unavailable.

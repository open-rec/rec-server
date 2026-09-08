package com.openrec.graph.node;

import static com.openrec.graph.RecParams.SIZE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.proto.model.ScoreResult;

/** Shared safe truncation and terminal result publication for recommendation graphs. */
public abstract class AbstractCollectorNode extends SyncNode<Void> {
    protected AbstractCollectorNode(NodeConfig nodeConfig) { super(nodeConfig); }
    protected abstract List<ScoreResult> candidates();
    protected void afterCollect(GraphContext context, List<ScoreResult> results) { }
    @Override public final void run(GraphContext context) {
        List<ScoreResult> values = candidates();
        if (values == null) values = Collections.emptyList();
        int requested = context.getParams().getValueToInt(SIZE);
        int limit = Math.min(values.size(), Math.max(0, requested));
        List<ScoreResult> results = new ArrayList<>(values.subList(0, limit));
        context.setResult(results);
        afterCollect(context, results);
    }
}

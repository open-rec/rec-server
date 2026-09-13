package com.openrec.graph.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable trace emitted once when a graph execution reaches a terminal state. */
public final class GraphExecutionTrace {
    private final GraphTraceContext context;
    private final long durationMillis;
    private final boolean deadlineExceeded;
    private final List<NodeExecutionTrace> nodes;

    public GraphExecutionTrace(GraphTraceContext context, long durationMillis, boolean deadlineExceeded,
        List<NodeExecutionTrace> nodes) {
        this.context = context;
        this.durationMillis = durationMillis;
        this.deadlineExceeded = deadlineExceeded;
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
    }

    public GraphTraceContext getContext() {
        return context;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public boolean isDeadlineExceeded() {
        return deadlineExceeded;
    }

    public List<NodeExecutionTrace> getNodes() {
        return nodes;
    }
}

package com.openrec.graph.trace;

@FunctionalInterface
public interface GraphTraceObserver {
    GraphTraceObserver NOOP = trace -> {
    };

    void onComplete(GraphExecutionTrace trace);
}

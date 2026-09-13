package com.openrec.graph.node;

/** Controls how the scheduler treats a node that fails or times out. */
public enum FailurePolicy {
    CONTINUE, SKIP_DESCENDANTS, FAIL_GRAPH
}

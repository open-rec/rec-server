package com.openrec.graph;

import com.openrec.graph.node.NodeStatus;

public class GraphExecutionException extends RuntimeException {

    private final String node;
    private final NodeStatus status;

    public GraphExecutionException(String node, NodeStatus status, Throwable cause) {
        super("graph node " + node + " finished with status " + status, cause);
        this.node = node;
        this.status = status;
    }

    public String getNode() {
        return node;
    }

    public NodeStatus getStatus() {
        return status;
    }
}

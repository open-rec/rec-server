package com.openrec.graph.trace;

import com.openrec.graph.node.NodeStatus;

/** Immutable timing and outcome information for one node execution. */
public final class NodeExecutionTrace {
    private final String nodeName;
    private final String nodeType;
    private final NodeStatus status;
    private final long queuedMillis;
    private final long executionMillis;
    private final int inputCount;
    private final int outputCount;
    private final String failureType;

    public NodeExecutionTrace(String nodeName, String nodeType, NodeStatus status, long queuedMillis,
        long executionMillis, int inputCount, int outputCount, String failureType) {
        this.nodeName = nodeName;
        this.nodeType = nodeType;
        this.status = status;
        this.queuedMillis = queuedMillis;
        this.executionMillis = executionMillis;
        this.inputCount = inputCount;
        this.outputCount = outputCount;
        this.failureType = failureType;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNodeType() {
        return nodeType;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public long getQueuedMillis() {
        return queuedMillis;
    }

    public long getExecutionMillis() {
        return executionMillis;
    }

    public int getInputCount() {
        return inputCount;
    }

    public int getOutputCount() {
        return outputCount;
    }

    public String getFailureType() {
        return failureType;
    }
}

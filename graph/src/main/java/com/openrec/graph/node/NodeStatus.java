package com.openrec.graph.node;

public enum NodeStatus {
    INIT, RUNNING, SUCCESS, FAILED, TIMED_OUT, CANCELLED, SKIPPED,
    /** @deprecated use SUCCESS; retained for source compatibility with older node implementations. */
    @Deprecated STOP;

    public boolean isTerminal() {
        return this != INIT && this != RUNNING;
    }
}

package com.openrec.graph.node;

public interface Node {

    String getId();
    NodeStatus getStatus();

    boolean finished();

    void destroy();
}

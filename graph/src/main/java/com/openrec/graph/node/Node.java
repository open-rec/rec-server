package com.openrec.graph.node;

public interface Node {

    String getId();

    String getName();
    NodeStatus getStatus();

    boolean finished();

    boolean isReady();

    public void addChild(Node child);

    public void addParent(Node parent);

    void destroy();
}

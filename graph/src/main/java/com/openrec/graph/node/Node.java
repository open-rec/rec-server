package com.openrec.graph.node;

import com.openrec.graph.GraphContext;

import java.util.List;

public interface Node {

    String getId();

    String getName();
    NodeStatus getStatus();

    boolean finished();

    boolean isRunning();

    boolean isReady();

    void start();

    void stop();

    void run(GraphContext context);

    void addChild(Node child);

    List<Node> getChildren();

    void addParent(Node parent);

    List<Node> getParents();

    void destroy();
}

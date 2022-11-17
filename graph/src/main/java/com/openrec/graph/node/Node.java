package com.openrec.graph.node;

import java.util.List;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;

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

    NodeConfig getConfig();

    void setConfig(NodeConfig config);

    int getTimeout();

    void refresh(NodeConfig config);

    void destroy();
}

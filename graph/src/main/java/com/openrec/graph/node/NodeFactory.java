package com.openrec.graph.node;

import com.openrec.graph.config.NodeConfig;

/** Creates one fresh node for each graph execution. */
public interface NodeFactory {

    String type();

    default void validate(NodeConfig config) {}

    Node create(NodeConfig config);
}

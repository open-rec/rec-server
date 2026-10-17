package com.openrec.graph.node.user;

import com.openrec.graph.config.NodeConfig;

/** Full-text recall over scene-scoped user profile documents. */
public class SearchNode extends com.openrec.graph.node.item.SearchNode {
    public SearchNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    protected String entityField() {
        return "user";
    }
}

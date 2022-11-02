package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;

public abstract class SyncNode<C> extends AbstractNode<C> {

    public SyncNode() {
        super();
    }

    public SyncNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public abstract void run(GraphContext context);
}

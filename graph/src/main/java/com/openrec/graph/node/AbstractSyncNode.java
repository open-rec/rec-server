package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;

public abstract class AbstractSyncNode<C> extends AbstractNode<C> {

    public AbstractSyncNode() {
        super();
    }

    public AbstractSyncNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public abstract void run(GraphContext context);
}

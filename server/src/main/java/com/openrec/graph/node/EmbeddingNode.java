package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;

// TODO: 2022/11/2 waiting for elastic search.
public class EmbeddingNode extends SyncNode<Void> {

    public EmbeddingNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {

    }
}

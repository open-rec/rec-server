package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;

// TODO: 2022/11/2 waiting for elastic search.
public class SearchNode extends SyncNode<Void> {
    public SearchNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }


    @Override
    public void run(GraphContext context) {

    }
}

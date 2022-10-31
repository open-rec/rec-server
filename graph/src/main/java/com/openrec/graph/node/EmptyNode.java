package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class EmptyNode extends SyncNode{


    public EmptyNode() {
        super();
    }

    public EmptyNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        log.info("empty node run");
    }
}

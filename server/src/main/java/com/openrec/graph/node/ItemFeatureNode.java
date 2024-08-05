package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * rank engine query item feature would provide better performance.
 */
@Slf4j
public class ItemFeatureNode extends SyncNode<Void> {

    public ItemFeatureNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {

    }
}

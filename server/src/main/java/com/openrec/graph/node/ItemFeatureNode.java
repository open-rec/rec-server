package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;

import lombok.extern.slf4j.Slf4j;

/** Independent item-feature preparation node reserved for the ranking stage. */
@Slf4j
public class ItemFeatureNode extends SyncNode<Void> {

    public ItemFeatureNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        // Standalone does not require ranking. Cluster ranking can export item features here.
    }
}

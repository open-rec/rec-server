package com.openrec.graph.node;

import org.junit.Test;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.HotConfig;
import com.openrec.graph.config.NodeConfig;

public class HotNodeTest {

    @Test
    public void run() {
        HotConfig hotConfig = new HotConfig();

        GraphContext context = new GraphContext();
        context.addParam("scene", "scene-1");

        NodeConfig<HotConfig> nodeConfig = new NodeConfig<>();
        nodeConfig.setContent(hotConfig);
        nodeConfig.setOpen(false);
        context.addConfig("hot", nodeConfig);

        HotNode hotNode = new HotNode(nodeConfig);
        hotNode.run(context);
    }
}
